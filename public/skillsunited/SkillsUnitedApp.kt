package com.dkvb.skillswap

import android.app.Application

class SkillsUnitedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)

        // Initialize App Check
        com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            )
    }
}