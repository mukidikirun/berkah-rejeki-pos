package com.berkahrejeki.pos

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    fun canUseBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
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
                    
                    // JIKA XIAOMI MEMBLOKIR POP-UP STANDAR, PAKSA BUKA KUNCI LAYAR (POLA/PIN)
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || 
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_VENDOR) {
                        
                        try {
                            val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                            if (km.isDeviceSecure) {
                                val intent = km.createConfirmDeviceCredentialIntent("Login Berkah Rejeki", "Gunakan Pola/PIN Tablet Anda")
                                // Memulai activity hasil dari keyguard
                                activity.startActivityForResult(intent, 999)
                                return@onAuthenticationError
                            }
                        } catch (e: Exception) {
                            // Abaikan jika gagal
                        }
                    }
                    
                    activity.runOnUiThread { onError(errString.toString()) }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Berkah Rejeki")
            .setSubtitle("Gunakan Pola/PIN Tablet Anda")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
