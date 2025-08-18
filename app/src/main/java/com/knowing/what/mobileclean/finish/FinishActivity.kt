package com.knowing.what.mobileclean.finish

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.knowing.what.mobileclean.img.ImageListActivity
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.big.BigFileListActivity
import com.knowing.what.mobileclean.databinding.ActivityFinishBinding
import com.knowing.what.mobileclean.junk.FileUtils
import com.knowing.what.mobileclean.junk.JunkActivity

class FinishActivity : AppCompatActivity() {
    private val binding by lazy { ActivityFinishBinding.inflate(layoutInflater) }

    private var countdownTimer: CountDownTimer? = null
    var jumpType = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cleanedSize = intent.getLongExtra("CLEANED_SIZE", 0L)
        jumpType = intent.getStringExtra("jump_type")?:""

        binding.tvSaveData.text = "Saved ${FileUtils.formatFileSize(cleanedSize)} space for you"

        showCleaningDialog()
        binding.dialogClean.tvBack.setOnClickListener {
            finish()
        }
        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.dialogClean.conClean.setOnClickListener {

        }
        binding.atvPicture.setOnClickListener {
            startActivity(Intent(this, ImageListActivity::class.java))
            finish()
        }
        binding.atvFile.setOnClickListener {
            startActivity(Intent(this, BigFileListActivity::class.java))
            finish()
        }
        binding.atvClean.setOnClickListener {
            startActivity(Intent(this, JunkActivity::class.java))
            finish()
        }

    }

    private fun showCleaningDialog() {
        val iamgeData =  if(jumpType == "image"){
            R.drawable.ic_img_logo
        }else if(jumpType == "file"){
            R.drawable.ic_file_logo
        }else{
            R.drawable.ic_app_logo
        }
        binding.dialogClean.imgLogo.setImageResource(iamgeData)
        binding.dialogClean.conClean.visibility = android.view.View.VISIBLE

        binding.dialogClean.pg.progress = 100

        countdownTimer = object : CountDownTimer(1000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                binding.dialogClean.pg.progress = 100 - (millisUntilFinished / 10).toInt()
            }

            override fun onFinish() {
                binding.dialogClean.conClean.visibility = android.view.View.GONE
            }
        }

        countdownTimer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
