package com.travelgo.app.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.travelgo.app.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thông tin người dùng hiển thị trên app. */
data class AppUser(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val provider: String, // "Google", "Facebook", "Khách"
)

/** Người dùng tự bấm huỷ khi chọn tài khoản: không cần báo lỗi. */
class SignInCancelledException : Exception()

/**
 * Đăng nhập bằng Firebase Auth (Người 1).
 *  - Google: Credential Manager lấy ID token, rồi đổi sang tài khoản Firebase
 *  - Khách: đăng nhập ẩn danh
 *  - TODO(Người 1): Facebook
 */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: AppUser? get() = auth.currentUser?.toAppUser()

    /** [activityContext] phải là context của Activity (LocalContext trong Compose). */
    suspend fun signInWithGoogle(activityContext: Context): Result<AppUser> = runCatching {
        // default_web_client_id do plugin google-services tự tạo từ google-services.json
        val webClientId = activityContext.getString(R.string.default_web_client_id)
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelledException()
        } catch (e: Exception) {
            Log.e("TravelGoAuth", "getCredential lỗi", e)
            throw e
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Kiểu đăng nhập không được hỗ trợ")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(firebaseCredential).awaitResult().user
            ?: error("Không lấy được tài khoản")
        user.toAppUser()
    }

    /**
     * Cách dự phòng khi Credential Manager báo NoCredentialException
     * (hay gặp trên máy ảo có Google Play services cũ): bảng chọn tài khoản Google kiểu cũ.
     */
    @Suppress("DEPRECATION")
    fun legacyGoogleSignInIntent(context: Context): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, options)
        client.signOut() // luôn cho chọn lại tài khoản
        return client.signInIntent
    }

    @Suppress("DEPRECATION")
    suspend fun finishLegacyGoogleSignIn(data: Intent?): Result<AppUser> = runCatching {
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(data).awaitResult()
        } catch (e: ApiException) {
            if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) throw SignInCancelledException()
            Log.e("TravelGoAuth", "Google Sign-In lỗi ${e.statusCode}", e)
            throw e
        }
        val idToken = account.idToken ?: error("Không lấy được ID token từ Google")
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitResult().user
            ?: error("Không lấy được tài khoản")
        user.toAppUser()
    }

    suspend fun signInAsGuest(): Result<AppUser> = runCatching {
        val user = auth.signInAnonymously().awaitResult().user ?: error("Không tạo được tài khoản khách")
        user.toAppUser()
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    /** Câu báo lỗi dễ hiểu cho người dùng; null nếu họ tự huỷ. */
    fun errorMessage(e: Throwable): String? = when (e) {
        is SignInCancelledException -> null
        is ApiException -> when (e.statusCode) {
            10 -> "Sai cấu hình Firebase (SHA-1 hoặc google-services.json chưa khớp). [mã 10]"
            7 -> "Không có kết nối mạng. [mã 7]"
            else -> "Đăng nhập chưa thành công. [mã ${e.statusCode}] ${e.message}"
        }
        is NoCredentialException ->
            "Không tìm thấy tài khoản Google phù hợp.\n[${e.javaClass.simpleName}] ${e.message}"
        else -> "Đăng nhập chưa thành công.\n[${e.javaClass.simpleName}] ${e.message}"
    }
}

private fun FirebaseUser.toAppUser(): AppUser {
    val providerIds = providerData.map { it.providerId }
    val provider = when {
        isAnonymous -> "Khách"
        "google.com" in providerIds -> "Google"
        "facebook.com" in providerIds -> "Facebook"
        else -> "Email"
    }
    return AppUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() } ?: if (isAnonymous) "Khách" else "Người dùng TravelGo",
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString(),
        provider = provider,
    )
}

/** Chờ Task của Firebase xong mà không cần thêm thư viện. */
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
