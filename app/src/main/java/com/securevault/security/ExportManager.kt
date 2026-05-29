package com.securevault.security
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor() {
    private val rng = SecureRandom()
    fun encrypt(json: String, password: String): String {
        val salt = ByteArray(16).also { rng.nextBytes(it) }
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val key = deriveKey(password.toCharArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val enc = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(salt + iv + enc, Base64.NO_WRAP)
    }
    fun decrypt(base64: String, password: String): String? = try {
        val data = Base64.decode(base64.trim(), Base64.NO_WRAP)
        val key = deriveKey(password.toCharArray(), data.copyOfRange(0, 16))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, data.copyOfRange(16, 28)))
        cipher.doFinal(data.copyOfRange(28, data.size)).toString(Charsets.UTF_8)
    } catch (_: Exception) { null }
    private fun deriveKey(pwd: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pwd, salt, 310_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
