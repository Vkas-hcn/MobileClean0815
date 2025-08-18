package com.knowing.what.mobileclean.main

import android.view.View


fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 500) {
            lastClickTime = currentTime
            action(v)
        }
    }
}

object ServiceLocator {

    private val services = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getService(clazz: Class<T>): T {
        return services[clazz] as? T
            ?: throw IllegalArgumentException("Service ${clazz.simpleName} not found")
    }

    fun <T> registerService(clazz: Class<T>, instance: T) {
        services[clazz] = instance as Any
    }

    fun clear() {
        services.clear()
    }
}