package com.lin0721.linmusic.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.feature.create.data.CreateRepository
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreateViewModel(
    private val createRepository: CreateRepository,
    userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userPreferences.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun createNewPlaylist(name: String, isPrivate: Boolean, onSuccess: () -> Unit = {}) {
        if (_isCreating.value) return
        if (name.isBlank()) {
            viewModelScope.launch { _toastEvent.emit("歌单名称不能为空") }
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            val privacy = if (isPrivate) 10 else 0
            createRepository.createPlaylist(name, privacy).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("歌单「$name」创建成功！")
                    onSuccess()
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
            _isCreating.value = false
        }
    }
}
