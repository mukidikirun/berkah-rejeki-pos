package com.berkahrejeki.pos

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    fun canUseBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    activity.runOnUiThread { onSuccess() }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity.runOnUiThread { onError(errString.toString()) }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Berkah Rejeki")
            .setSubtitle("Tempelkan sidik jari Anda untuk masuk")
            .setNegativeButtonText("Gunakan Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
