package com.lin0721.linmusic.di

import com.lin0721.linmusic.data.local.PlaybackPreferences
import com.lin0721.linmusic.data.local.SettingsPreferences
import com.lin0721.linmusic.data.local.UserPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localModule = module {
    single { PlaybackPreferences(androidContext()) }
    single { UserPreferences(androidContext()) }
    single { SettingsPreferences(androidContext()) }
}
