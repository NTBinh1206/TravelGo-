package com.travelgo.app.ui.auth

import android.content.Context
import android.content.Intent
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.travelgo.app.data.auth.AppUser
import com.travelgo.app.data.auth.AuthRepository
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    /** Đăng nhập xong: màn hình chuyển sang bước xin quyền vị trí. */
    val loggedIn: Boolean = false,
    /** Máy không hỗ trợ Credential Manager: màn hình cần mở bảng chọn tài khoản Google kiểu cũ. */
    val needLegacyGoogle: Boolean = false,
)

/**
 * ViewModel màn 01 · Đăng nhập (Người 1).
 * Giữ trạng thái đang tải / lỗi và gọi AuthRepository. Các hộp thoại của Google, Facebook
 * bắt buộc phải mở từ Activity nên vẫn nằm ở màn hình, kết quả được báo về đây.
 */
class LoginViewModel(
    private val auth: AuthRepository = AppContainer.authRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Bắt đầu một lần đăng nhập; trả về false nếu đang có lần khác chạy. */
    private fun begin(): Boolean {
        if (_uiState.value.loading) return false
        _uiState.update { it.copy(loading = true, error = null) }
        return true
    }

    private fun finish(result: Result<AppUser>) {
        result
            .onSuccess { _uiState.update { it.copy(loading = false, loggedIn = true) } }
            .onFailure { e -> _uiState.update { it.copy(loading = false, error = auth.errorMessage(e)) } }
    }

    /**
     * Đăng nhập Google bằng Credential Manager. [activityContext] chỉ dùng trong lúc gọi, không giữ lại
     * (Credential Manager cần Activity để hiện bảng chọn tài khoản).
     */
    fun signInWithGoogle(activityContext: Context) {
        if (!begin()) return
        viewModelScope.launch {
            val result = auth.signInWithGoogle(activityContext)
            val error = result.exceptionOrNull()
            if (error is NoCredentialException) {
                // Máy không hỗ trợ cách mới: nhờ màn hình mở bảng chọn tài khoản kiểu cũ.
                _uiState.update { it.copy(needLegacyGoogle = true) }
            } else {
                finish(result)
            }
        }
    }

    fun legacyGoogleIntent(context: Context): Intent = auth.legacyGoogleSignInIntent(context)

    fun onLegacyGoogleLaunched() {
        _uiState.update { it.copy(needLegacyGoogle = false) }
    }

    fun onLegacyGoogleResult(data: Intent?) {
        viewModelScope.launch { finish(auth.finishLegacyGoogleSignIn(data)) }
    }

    /** Gọi trước khi mở màn đăng nhập Facebook; false nghĩa là đang bận, không mở. */
    fun startFacebook(): Boolean = begin()

    fun onFacebookSuccess(token: AccessToken) {
        viewModelScope.launch { finish(auth.signInWithFacebook(token)) }
    }

    fun onFacebookCancel() {
        _uiState.update { it.copy(loading = false) }
    }

    fun onFacebookError(message: String?) {
        _uiState.update {
            it.copy(loading = false, error = "Đăng nhập Facebook chưa thành công.\n${message.orEmpty()}".trim())
        }
    }

    fun signInAsGuest() {
        if (!begin()) return
        viewModelScope.launch { finish(auth.signInAsGuest()) }
    }
}
