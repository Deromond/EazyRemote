package com.easy.peasy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity


class WebActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"      // передає Door
        const val EXTRA_KILL = "kill"     // kill-path
        const val EXTRA_GAME = "game_cls" // повна назва класу ігрової активності
    }

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baked) // твій існуючий layout із WebView@id/web
        enableFullScreen()

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val kill = intent.getStringExtra(EXTRA_KILL) ?: ""
        val gameClsName = intent.getStringExtra(EXTRA_GAME) ?: MainActivity::class.java.name
        val gameCls = Class.forName(gameClsName) as Class<out Activity>

        web = findViewById(R.id.web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, u: String?) {
                super.onPageFinished(view, u)
                if (!u.isNullOrBlank() && kill.isNotBlank() && u.contains(
                        kill,
                        ignoreCase = true
                    )
                ) {
                    // kill → відкриваємо гру, прибираємо splash/loader

                    startActivity(Intent(this@WebActivity, gameCls))
                    SplashKeeper.finishIfAlive()
                    finish()
                }
                // Log.d("Door", "web link: $u")
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Наприклад: "net::ERR_ABORTED" або код+опис
                        "${error.errorCode}: ${error.description}"
                    } else {
                        "Page load error"
                    }
                    showToast(msg)                    // 👈 показуємо, що сталося
                    startActivity(Intent(this@WebActivity, gameCls))
                    SplashKeeper.finishIfAlive()
                    finish()
                }
            }

            // HTTP errors (є відповідь, але погана)
            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    val reason = errorResponse.reasonPhrase ?: "HTTP error"
                    showToast("HTTP ${errorResponse.statusCode}: $reason")
                    startActivity(Intent(this@WebActivity, gameCls))
                    SplashKeeper.finishIfAlive()
                    finish()
                }
            }

            // Якщо рендер крешнувся — рятуємось у апку
            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                showToast("Web content crashed")
                startActivity(Intent(this@WebActivity, gameCls))
                SplashKeeper.finishIfAlive()
                finish()
                return true
            }


            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): Boolean {
                val u = request?.url?.toString() ?: return false
                if (kill.isNotBlank() && u.contains(kill, ignoreCase = true)) {
                    startActivity(Intent(this@WebActivity, gameCls))
                    SplashKeeper.finishIfAlive()
                    finish()
                    return true
                }
                return false
            }
        }



        onBackPressedDispatcher.addCallback(this) {
            if (web.canGoBack()) web.goBack()
        }

        web.loadUrl(url)
    }


    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun enableFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { c ->
                c.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }
}

object SplashKeeper {
    private var ref: java.lang.ref.WeakReference<Activity>? = null
    fun hold(a: Activity) {
        ref = java.lang.ref.WeakReference(a)
    }

    fun finishIfAlive() {
        ref?.get()?.run { if (!isFinishing) finish() }; ref = null
    }
}
