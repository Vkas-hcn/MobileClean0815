package com.knowing.what.mobileclean.main

import android.content.Context
import android.content.Intent
import com.knowing.what.mobileclean.big.BigFileListActivity
import com.knowing.what.mobileclean.img.ImageListActivity
import com.knowing.what.mobileclean.junk.JunkActivity
import com.knowing.what.mobileclean.ohther.ShowSetActivity

interface NavigationCallback {
    fun onNavigationRequested(action: NavigationAction)
}

class NavigationManager(private val context: Context) {

    fun navigate(action: NavigationAction) {
        val intent = when (action) {
            is NavigationAction.JunkClean -> Intent(context, JunkActivity::class.java)
            is NavigationAction.ImageClean -> Intent(context, ImageListActivity::class.java)
            is NavigationAction.FileClean -> Intent(context, BigFileListActivity::class.java)
            is NavigationAction.Settings -> Intent(context, ShowSetActivity::class.java)
        }
        context.startActivity(intent)
    }
}