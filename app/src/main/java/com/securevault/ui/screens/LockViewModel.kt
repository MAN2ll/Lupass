package com.securevault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.data.repository.VaultRepository
import com.securevault.security.CryptoManager
import com.securevault.security.PasswordHasher
import com.securevault.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LockState {
    object Idle : LockState()
    object Loading : LockState()
    object Success : LockState()
    object Wiped : LockState()
    data class Error(val msg: String) : LockState()
    data class LockedOut(val secondsLeft: Long, val attemptsLeft: Int) : LockState()
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val session: SessionManager,
    private val hasher: PasswordHasher,
    private val repo: VaultRepository,
    private val crypto: CryptoManager
) : ViewModel() {
    private val _state = MutableStateFlow<LockState>(LockState.Idle)
    val state: StateFlow<LockState> = _state

    val isSetupDone get() = session.isSetupDone()
    val isBiometricEnabled get() = session.isBiometricEnabled()
    val failedAttempts get() = session.getFailedAttempts()

    init {
        // При открытии проверяем не заблокирован ли
        if (session.isLockedOut()) startLockoutTimer()
    }

    fun setup(pwd: String, confirm: String) {
        if (pwd.length < 6) { _state.value = LockState.Error("Минимум 6 символов"); return }
        if (pwd != confirm) { _state.value = LockState.Error("Пароли не совпадают"); return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = LockState.Loading
            runCatching { hasher.hash(pwd) }
                .onSuccess { h -> session.saveMasterHash(h); session.unlock(); _state.value = LockState.Success }
                .onFailure { _state.value = LockState.Error("Ошибка создания") }
        }
    }

    fun unlock(pwd: String) {
        if (session.isLockedOut()) { startLockoutTimer(); return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = LockState.Loading
            val hash = session.getMasterHash()
            val ok = hash != null && runCatching { hasher.verify(pwd, hash) }.getOrDefault(false)
            if (ok) {
                session.unlock()
                _state.value = LockState.Success
            } else {
                val fails = session.incrementFails()
                if (fails >= 10) {
                    // САМОУНИЧТОЖЕНИЕ
                    runCatching { repo.deleteAll(); crypto.deleteKey(); session.reset() }
                    _state.value = LockState.Wiped
                } else {
                    if (session.isLockedOut()) startLockoutTimer()
                    else {
                        val left = 10 - fails
                        _state.value = LockState.Error("Неверный пароль. Осталось попыток: $left\n(после 10 — хранилище будет удалено)")
                    }
                }
            }
        }
    }

    fun unlockBio() { session.unlock(); _state.value = LockState.Success }
    fun clearError() { if (_state.value is LockState.Error) _state.value = LockState.Idle }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            while (session.isLockedOut()) {
                val sec = session.getLockRemainingSeconds()
                val left = 10 - session.getFailedAttempts()
                _state.value = LockState.LockedOut(sec, left)
                delay(1000)
            }
            if (_state.value is LockState.LockedOut) _state.value = LockState.Idle
        }
    }
}
