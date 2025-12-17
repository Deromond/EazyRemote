package com.easy.peasy

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.easy.peasy.databinding.ActivityTestBinding
import kotlin.random.Random

class TestActivity : AppCompatActivity() {
    private lateinit var b: ActivityTestBinding
    private var blockId: Int = 0
    private lateinit var order: MutableList<Int>
    private var idx = 0
    private var correct = 0
    private var xp = 0
    private var timer: CountDownTimer? = null
    private var revealTimer: CountDownTimer? = null
    private var chosenIndex: Int? = null

    // для шафлу відповідей
    private var optShuffle: List<Int> = emptyList()
    private var correctMappedIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTestBinding.inflate(layoutInflater)
        setContentView(b.root)
        immersiveFullscreen()

        blockId = intent.getIntExtra("block_id", 1)
        val block = QuestionsProvider.getBlock(blockId) ?: return finish()

        // рандом порядку питань
        order = block.questions.indices.shuffled(Random(System.currentTimeMillis())).toMutableList()

        b.btnClose.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Выйти? Прогресс этого теста будет потерян.")
                .setPositiveButton("Да") { _, _ -> finish() }
                .setNegativeButton("Нет", null)
                .show()
        }
        onBackPressedDispatcher.addCallback(this) { }
        showQuestion()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersiveFullscreen()
    }

    private fun showQuestion() {
        timer?.cancel()
        revealTimer?.cancel()
        chosenIndex = null

        val block = QuestionsProvider.getBlock(blockId)!!
        if (idx >= order.size) {
            val i = Intent(this, ResultActivity::class.java)
            i.putExtra("block_id", blockId)
            i.putExtra("correct", correct)
            i.putExtra("total", order.size)
            i.putExtra("xp", xp)
            startActivity(i)
            finish()
            return
        }

        val q = block.questions[order[idx]]
        b.counter.text = "${idx + 1}/${order.size}"
        b.question.text = q.text

        // ---- ШАФЛ ВІДПОВІДЕЙ ----
        optShuffle = (0..3).shuffled(Random(System.currentTimeMillis()))
        correctMappedIndex = optShuffle.indexOf(q.correctIndex)

        val opts = listOf(b.opt1, b.opt2, b.opt3, b.opt4)
        opts.forEachIndexed { i, tv ->
            tv.text = q.options[optShuffle[i]]
            tv.isEnabled = true
            tv.isSelected = false
            tv.isActivated = false
            tv.setOnClickListener {
                chosenIndex = i
                opts.forEach { it.isSelected = false }
                tv.isSelected = true // тільки рамка/підсвітка
            }
        }

        // панель “Узнать результат”
        b.resultPanel.visibility = View.GONE
        b.resultText.text = "Узнать результат"
        b.resultCountdown.text = ""

        startTimer()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(8_000, 1000) {
            override fun onTick(ms: Long) {
                val secLeft = (ms / 1000).toInt()
                b.timerText.text = secLeft.toString()
                if (secLeft <= 5) {
                    b.resultPanel.visibility = View.VISIBLE
                    b.resultCountdown.text = secLeft.toString()
                }
            }

            override fun onFinish() {
                // оцінюємо відповідь з урахуванням МАПІНГУ
                val isCorrect = (chosenIndex == correctMappedIndex)
                if (isCorrect) {
                    correct += 1
                    xp += 10
                }

                // підсвітити правильний
                val all = listOf(b.opt1, b.opt2, b.opt3, b.opt4)
                all.forEachIndexed { i, v ->
                    v.isEnabled = false
                    v.isSelected = false
                    v.isActivated = (i == correctMappedIndex) // зелена товста рамка
                }

                b.resultPanel.visibility = View.VISIBLE
                b.resultText.text = "Верный ответ: " + all[correctMappedIndex].text
                b.resultCountdown.text = ""

                revealTimer = object : CountDownTimer(2000, 2000) {
                    override fun onTick(ms: Long) {}
                    override fun onFinish() {
                        idx += 1
                        showQuestion()
                    }
                }.start()
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        revealTimer?.cancel()
    }
}
