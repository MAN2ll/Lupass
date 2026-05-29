package com.securevault.security
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {
    private val rng = SecureRandom()
    fun hash(password: String): String {
        val salt = ByteArray(16).also { rng.nextBytes(it) }
        val hash = derive(password.toCharArray(), salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        val actual = derive(password.toCharArray(), salt)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }
    private fun derive(p: CharArray, s: ByteArray): ByteArray {
        val spec = PBEKeySpec(p, s, 310_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
