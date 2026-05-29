package com.securevault.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        ctx, "sv_prefs",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Volatile private var unlocked = false

    fun isSetupDone() = prefs.getBoolean("setup", false)
    fun getMasterHash(): String? = prefs.getString("hash", null)
    fun saveMasterHash(h: String) { prefs.edit().putString("hash", h).putBoolean("setup", true).apply() }
    fun unlock() { unlocked = true; prefs.edit().putInt("fails", 0).putLong("lockUntil", 0).apply() }
    fun lock() { unlocked = false }
    fun isUnlocked() = unlocked
    fun isBiometricEnabled() = prefs.getBoolean("bio", false)
    fun setBiometricEnabled(v: Boolean) { prefs.edit().putBoolean("bio", v).apply() }

    /** Возвращает количество неудачных попыток после инкремента */
    fun incrementFails(): Int {
        val n = prefs.getInt("fails", 0) + 1
        prefs.edit().putInt("fails", n).apply()
        // Прогрессивная блокировка
        val lockMs = when {
            n >= 10 -> Long.MAX_VALUE  // самоуничтожение — обрабатывается в ViewModel
            n >= 7  -> 5 * 60_000L
            n >= 5  -> 30_000L
            n >= 3  -> 10_000L
            else    -> 0L
        }
        if (lockMs > 0) prefs.edit().putLong("lockUntil", System.currentTimeMillis() + lockMs).apply()
        return n
    }

    fun getFailedAttempts() = prefs.getInt("fails", 0)
    fun getLockUntil() = prefs.getLong("lockUntil", 0L)
    fun isLockedOut() = System.currentTimeMillis() < getLockUntil()
    fun getLockRemainingSeconds() = maxOf(0L, (getLockUntil() - System.currentTimeMillis()) / 1000L)

    fun reset() { prefs.edit().clear().apply(); unlocked = false }
}
