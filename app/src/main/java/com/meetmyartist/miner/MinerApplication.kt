package com.meetmyartist.miner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MinerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        com.google.firebase.FirebaseApp.initializeApp(this)
        // Crashlytics auto-initializes if in dependencies
    }
}
