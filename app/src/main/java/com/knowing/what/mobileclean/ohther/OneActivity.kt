package com.knowing.what.mobileclean.ohther

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ActivityOneBinding
import com.knowing.what.mobileclean.main.MainActivity

class OneActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOneBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.one)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback {
        }
        startCountdown()
    }

    private fun startCountdown() {
        object : CountDownTimer(2000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                var pro= 100-millisUntilFinished * 0.05
                binding.progressBar.progress = pro.toInt()
            }

            override fun onFinish() {
                startActivity(Intent(this@OneActivity, MainActivity::class.java))
                finish()
            }
        }.start()
    }
}