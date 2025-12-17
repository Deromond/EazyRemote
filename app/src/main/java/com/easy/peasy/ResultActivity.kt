package com.easy.peasy

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.easy.peasy.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var b: ActivityResultBinding
    private var blockId: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityResultBinding.inflate(layoutInflater)
        setContentView(b.root)
        onBackPressedDispatcher.addCallback(this) { }
        blockId = intent.getIntExtra("block_id", 1)
        val correct = intent.getIntExtra("correct", 0)
        val total = intent.getIntExtra("total", 0)
        val xp = intent.getIntExtra("xp", 0)

        XPManager.add(blockId, xp, this)

        b.resultTitle.text = "Результат"
        b.scoreText.text = "+%d xp".format(xp)
        b.detailsText.text = "Вы ответили правильно на %d/%d вопросов!".format(correct, total)

        b.btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        b.btnAgain.setOnClickListener {
            XPManager.reset(blockId, this)
            val i = Intent(this, TestActivity::class.java)
            i.putExtra("block_id", blockId)
            startActivity(i)
            finish()
        }
        immersiveFullscreen()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersiveFullscreen()
    }

}