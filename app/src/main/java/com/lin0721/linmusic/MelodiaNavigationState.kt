package com.lin0721.linmusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Screen {
    Home, Playlist, Search, Library, Settings, Artist
}

// 应用级导航状态：回退栈与各页面所需的跳转参数
class MelodiaNavigationState {

    private val backStack = mutableStateListOf(Screen.Home)

    val currentScreen: Screen by derivedStateOf { backStack.lastOrNull() ?: Screen.Home }

    // 栈深大于 1 时才有上一级可回退
    val canNavigateBack: Boolean get() = backStack.size > 1

    var activePlaylistId by mutableStateOf<Long?>(null)
        private set

    var activePlaylistIsAlbum by mutableStateOf(false)
        private set

    var activeArtistId by mutableStateOf<Long?>(null)
        private set

    var searchAutoFocus by mutableStateOf(false)
        private set

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() == screen) return
        when (screen) {
            // 主页为栈底，跳转时清空历史
            Screen.Home -> {
                backStack.clear()
                backStack.add(Screen.Home)
            }
            // 底栏一级入口，始终保留主页作为回退目标
            Screen.Search, Screen.Library -> {
                backStack.clear()
                backStack.add(Screen.Home)
                backStack.add(screen)
            }
            else -> backStack.add(screen)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun openPlaylist(id: Long, isAlbum: Boolean) {
        activePlaylistId = id
        activePlaylistIsAlbum = isAlbum
        navigateTo(Screen.Playlist)
    }

    fun openArtist(id: Long) {
        activeArtistId = id
        navigateTo(Screen.Artist)
    }

    // 从主页搜索框进入时自动弹键盘，从底栏进入时展示发现内容
    fun openSearch(autoFocus: Boolean) {
        searchAutoFocus = autoFocus
        navigateTo(Screen.Search)
    }

    // 底栏一级入口跳转，进入搜索页时不自动弹键盘
    fun openTab(screen: Screen) {
        searchAutoFocus = false
        navigateTo(screen)
    }
}

@Composable
fun rememberMelodiaNavigationState(): MelodiaNavigationState = remember { MelodiaNavigationState() }
