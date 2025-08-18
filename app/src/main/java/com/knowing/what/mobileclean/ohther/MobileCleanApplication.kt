package com.knowing.what.mobileclean.ohther

import android.app.Application
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.big.FileItem
import com.knowing.what.mobileclean.big.FileType
import com.knowing.what.mobileclean.main.ServiceLocator
import com.knowing.what.mobileclean.main.StorageInfoManager
import java.io.File

class MobileCleanApplication : Application() {
    companion object {
        lateinit var instance: MobileCleanApplication
    }
    override fun onCreate() {
        super.onCreate()
        instance =this
        ServiceLocator.registerService(
            StorageInfoManager::class.java,
            StorageInfoManager(this)
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        ServiceLocator.clear()
    }
}