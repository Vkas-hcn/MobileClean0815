package com.knowing.what.mobileclean.finish

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
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
import com.knowing.what.mobileclean.main.NavigationAction

class FinishActivity : AppCompatActivity() {
    private val binding by lazy { ActivityFinishBinding.inflate(layoutInflater) }

    private var countdownTimer: CountDownTimer? = null
    var jumpType = ""
    private var scanCountDownTimer: CountDownTimer? = null
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
            showScanDialogAndNavigate(NavigationAction.ImageClean)

        }
        binding.atvFile.setOnClickListener {
            showScanDialogAndNavigate(NavigationAction.FileClean)

        }
        binding.atvClean.setOnClickListener {
            startActivity(Intent(this, JunkActivity::class.java))
            finish()
        }

    }
    private fun showScanDialogAndNavigate(action: NavigationAction) {

        val iamgeData = when (action) {
            NavigationAction.ImageClean -> {
                R.drawable.ic_img_logo
            }
            NavigationAction.FileClean -> {
                R.drawable.ic_file_logo
            }
            else -> {
                R.drawable.ic_app_logo
            }
        }
        binding.dialogClean.imgLogo.setImageResource(iamgeData)
        binding.dialogClean.conClean.visibility = View.VISIBLE
        binding.dialogClean.tvTip.text = "Scaning"

        scanCountDownTimer?.cancel()
        scanCountDownTimer = object : CountDownTimer(1000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((1000 - millisUntilFinished) / 10).toInt()
                binding.dialogClean.pg.progress = progress
            }

            override fun onFinish() {
                binding.dialogClean.conClean.visibility = View.GONE
                when (action) {
                    NavigationAction.ImageClean -> {
                        startActivity(Intent(this@FinishActivity, ImageListActivity::class.java))
                    }
                    NavigationAction.FileClean -> {
                        startActivity(Intent(this@FinishActivity, BigFileListActivity::class.java))
                    }
                    else -> {
                        startActivity(Intent(this@FinishActivity, BigFileListActivity::class.java))
                    }
                }
                finish()
            }
        }.start()
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
