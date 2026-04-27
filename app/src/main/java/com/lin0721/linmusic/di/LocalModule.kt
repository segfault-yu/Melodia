package com.lin0721.linmusic.di

import com.lin0721.linmusic.data.local.PlaybackPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localModule = module {
    single { PlaybackPreferences(androidContext()) }
}
