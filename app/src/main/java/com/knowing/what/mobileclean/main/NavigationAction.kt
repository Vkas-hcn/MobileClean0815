package com.knowing.what.mobileclean.main

sealed class NavigationAction {
    object JunkClean : NavigationAction()
    object ImageClean : NavigationAction()
    object FileClean : NavigationAction()
    object Settings : NavigationAction()
}