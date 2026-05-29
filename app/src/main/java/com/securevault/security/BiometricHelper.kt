package com.securevault.security
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricHelper @Inject constructor() {
    fun isAvailable(activity: FragmentActivity) =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(code: Int, msg: CharSequence) = onError(msg.toString())
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Secure Vault").setSubtitle("Войдите по биометрии")
                .setNegativeButtonText("Ввести пароль")
                .setAllowedAuthenticators(BIOMETRIC_STRONG).build()
        )
    }
}
