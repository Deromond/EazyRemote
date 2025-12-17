package com.easy.peasy

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.easy.peasy.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        render()
        b.level1.setOnClickListener { openBlock(1) }
        b.level2.setOnClickListener { openBlock(2) }
        b.level3.setOnClickListener { openBlock(3) }

        immersiveFullscreen()
        // Ініціалізація AdMob
        MobileAds.initialize(this)
        // Показ банера
        val adView = findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        onBackPressedDispatcher.addCallback(this) { }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersiveFullscreen()
    }


    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        b.title.text = "Выберите уровень"
        QuestionsProvider.blocks.forEach {
            when (it.id) {
                1 -> { b.level1Title.text = it.title; b.level1Xp.text = "${XPManager.get(1,this)} xp" }
                2 -> { b.level2Title.text = it.title; b.level2Xp.text = "${XPManager.get(2,this)} xp" }
                3 -> { b.level3Title.text = it.title; b.level3Xp.text = "${XPManager.get(3,this)} xp" }
            }
        }
    }

    private fun openBlock(id: Int) {
        val i = Intent(this, TestActivity::class.java)
        i.putExtra("block_id", id)
        startActivity(i)
    }
}