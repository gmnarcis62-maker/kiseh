package com.example.backup

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * مدیریت احراز هویت واقعی با Firebase Authentication (ایمیل/رمز عبور)
 */
class FirebaseAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseAuth"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _accountState = MutableStateFlow<GoogleAccountState>(GoogleAccountState.Disconnected)
    val accountState: StateFlow<GoogleAccountState> = _accountState.asStateFlow()

    init {
        // همگام‌سازی وضعیت با تغییرات FirebaseAuth
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _accountState.value = if (user != null) {
                GoogleAccountState.Connected(
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email?.substringBefore("@")
                )
            } else {
                GoogleAccountState.Disconnected
            }
        }
    }

    /**
     * ورود با ایمیل و رمز عبور
     */
    suspend fun signIn(email: String, password: String): Result<GoogleAccountState> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
                ?: return Result.failure(Exception("ورود ناموفق: کاربر یافت نشد"))
            Log.i(TAG, "Sign in successful: ${user.email}")
            Result.success(
                GoogleAccountState.Connected(
                    email = user.email ?: email,
                    displayName = user.displayName ?: user.email?.substringBefore("@")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(Exception(translateAuthError(e.message)))
        }
    }

    /**
     * ثبت‌نام با ایمیل، رمز عبور و نام نمایشی
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<GoogleAccountState> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
                ?: return Result.failure(Exception("ثبت‌نام ناموفق: کاربر ساخته نشد"))

            if (displayName.isNotBlank()) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set display name", e)
                }
            }

            Log.i(TAG, "Sign up successful: ${user.email}")
            Result.success(
                GoogleAccountState.Connected(
                    email = user.email ?: email,
                    displayName = displayName.ifBlank { user.email?.substringBefore("@") }
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(Exception(translateAuthError(e.message)))
        }
    }

    /**
     * خروج از حساب
     */
    fun signOut() {
        auth.signOut()
        Log.i(TAG, "User signed out")
    }

    /**
     * شناسه کاربر جاری
     */
    fun getUserId(): String? = auth.currentUser?.uid

    /**
     * ایمیل کاربر جاری
     */
    fun getUserEmail(): String? = auth.currentUser?.email

    /**
     * بررسی اینکه کاربر وارد شده یا نه
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * ترجمه پیام خطای Firebase به فارسی
     */
    private fun translateAuthError(message: String?): String {
        if (message == null) return "خطای ناشناخته در احراز هویت"
        return when {
            message.contains("password is invalid", ignoreCase = true) ||
                    message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "ایمیل یا رمز عبور نادرست است"
            message.contains("no user record", ignoreCase = true) ->
                "حسابی با این ایمیل یافت نشد. ابتدا ثبت‌نام کنید"
            message.contains("email address is already in use", ignoreCase = true) ->
                "این ایمیل قبلاً ثبت شده است. وارد شوید"
            message.contains("password should be at least", ignoreCase = true) ->
                "رمز عبور باید حداقل ۶ کاراکتر باشد"
            message.contains("badly formatted", ignoreCase = true) ||
                    message.contains("invalid email", ignoreCase = true) ->
                "فرمت ایمیل نامعتبر است"
            message.contains("network", ignoreCase = true) ->
                "خطای اتصال به اینترنت"
            message.contains("too many requests", ignoreCase = true) ->
                "تلاش‌های زیاد ناموفق. لطفاً چند دقیقه بعد امتحان کنید"
            else -> message
        }
    }
}