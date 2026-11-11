package com.travelgo.app.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.auth.AppUser
import com.travelgo.app.data.auth.AuthRepository
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: AppUser? = null,
    val signingOut: Boolean = false,
    /** Đã đăng xuất xong: màn hình quay về màn Đăng nhập. */
    val signedOut: Boolean = false,
)

/** ViewModel màn 05 · Cá nhân (Người 1): thông tin tài khoản và đăng xuất. */
class ProfileViewModel(
    private val auth: AuthRepository = AppContainer.authRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(user = auth.currentUser))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Đăng xuất ở cả Firebase, Facebook và Google. [context] chỉ dùng trong lúc gọi, không giữ lại. */
    fun signOut(context: Context) {
        if (_uiState.value.signingOut) return
        _uiState.update { it.copy(signingOut = true) }
        viewModelScope.launch {
            auth.signOut(context)
            _uiState.update { it.copy(signingOut = false, signedOut = true) }
        }
    }
}
