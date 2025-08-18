package com.knowing.what.mobileclean.main

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), PermissionCallback {

    private lateinit var binding: ActivityMainBinding

    private val permissionDelegate = PermissionDelegate(this, this)

    private val permissionManager by permissionDelegate
    private var scanCountDownTimer: CountDownTimer? = null
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            StorageInfoManager(this),
            NavigationManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager.hasStoragePermission() // This triggers the delegate
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setupUI()
        setupObservers()
        setupClickListeners()

        viewModel.updateStorageInfo()
    }

    override fun onResume() {
        super.onResume()
        if (permissionManager.hasStoragePermission()) {
            binding.permissDialog.isVisible = false
        }
        viewModel.updateStorageInfo()
    }

    private fun setupUI() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel // 绑定ViewModel到布局

        supportActionBar?.hide()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setStatusBarHeightPadding()
    }
    private fun setStatusBarHeightPadding() {
        binding.titleBar.viewTreeObserver.addOnGlobalLayoutListener {
            val statusBarHeight = getStatusBarHeight()
            binding.titleBar.setPadding(0, statusBarHeight, 0, 0)
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            val scale = resources.displayMetrics.density
            (24 * scale + 0.5f).toInt()
        }
    }
    private fun setupObservers() {
        viewModel.storageInfo.observe(this) { storageInfo ->
            binding.storageInfo = storageInfo
        }

        viewModel.onPermissionRequired = {
            showPermissionDialog()
        }
    }

    private fun setupClickListeners() {
        binding.flClean.setOnClickListener {
            viewModel.requestNavigation(
                NavigationAction.JunkClean,
                permissionManager.hasStoragePermission()
            )
        }

        binding.llImage.setOnClickListener {
            showScanDialogAndNavigate(NavigationAction.ImageClean)
        }

        binding.llFile.setOnClickListener {
            showScanDialogAndNavigate(NavigationAction.FileClean)
        }

        binding.settingsIcon.setOnClickListener {
            viewModel.requestNavigation(
                NavigationAction.Settings,
                true // 设置页面不需要权限
            )
        }

        binding.permissDialog.setOnClickListener {
        }

        binding.tvCancel.setOnClickListener {
            binding.permissDialog.isVisible = false
            permissionManager.showPermissionDeniedDialog()
        }

        binding.tvYes.setOnClickListener {
            binding.permissDialog.isVisible = false
            permissionManager.requestStoragePermission()
        }
    }
    private fun showScanDialogAndNavigate(action: NavigationAction) {
        if (!permissionManager.hasStoragePermission()) {
            showPermissionDialog()
            return
        }
       val iamgeData =  if(action == NavigationAction.ImageClean){
           R.drawable.ic_img_logo
        }else if(action == NavigationAction.FileClean){
            R.drawable.ic_file_logo
        }else{
            R.drawable.ic_app_logo
        }
        binding.inScan.imgLogo.setImageResource(iamgeData)
        binding.inScan.conClean.visibility = View.VISIBLE
        binding.inScan.tvTip.text = "Scaning"

        scanCountDownTimer?.cancel()
        scanCountDownTimer = object : CountDownTimer(1000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((1000 - millisUntilFinished) / 10).toInt()
                binding.inScan.pg.progress = progress
            }

            override fun onFinish() {
                binding.inScan.conClean.visibility = View.GONE
                viewModel.requestNavigation(action, true)
            }
        }.start()
    }
    private fun showPermissionDialog() {
        binding.permissDialog.isVisible = true
    }

    override fun onPermissionGranted() {
        binding.permissDialog.isVisible = false
        viewModel.onPermissionGranted()
    }

    override fun onPermissionDenied(shouldShowRationale: Boolean) {
        if (shouldShowRationale) {
            showPermissionDialog()
        } else {
            permissionManager.showPermissionDeniedDialog()
        }
    }
}