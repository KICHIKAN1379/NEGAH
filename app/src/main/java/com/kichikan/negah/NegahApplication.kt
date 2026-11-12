package com.kichikan.negah

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NegahApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BirthdayReminderWorker.schedule(this)
    }
}
