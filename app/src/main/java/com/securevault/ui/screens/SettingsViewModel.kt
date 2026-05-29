package com.securevault.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.data.repository.Entry
import com.securevault.data.repository.VaultRepository
import com.securevault.security.CryptoManager
import com.securevault.security.ExportManager
import com.securevault.security.PasswordHasher
import com.securevault.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

sealed class SettingsState {
    object Idle : SettingsState()
    object Loading : SettingsState()
    data class Success(val msg: String) : SettingsState()
    data class Error(val msg: String) : SettingsState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val session: SessionManager,
    private val hasher: PasswordHasher,
    private val repo: VaultRepository,
    private val exportMgr: ExportManager,
    private val crypto: CryptoManager
) : ViewModel() {
    private val _state = MutableStateFlow<SettingsState>(SettingsState.Idle)
    val state: StateFlow<SettingsState> = _state
    val isBiometricEnabled get() = session.isBiometricEnabled()

    fun setBiometric(on: Boolean) { session.setBiometricEnabled(on) }

    fun changePassword(cur: String, new: String, confirm: String) {
        if (new.length < 6) { _state.value = SettingsState.Error("Минимум 6 символов"); return }
        if (new != confirm)  { _state.value = SettingsState.Error("Пароли не совпадают"); return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            val hash = session.getMasterHash()
            val ok = hash != null && runCatching { hasher.verify(cur, hash) }.getOrDefault(false)
            if (!ok) { _state.value = SettingsState.Error("Неверный текущий пароль"); return@launch }
            session.saveMasterHash(hasher.hash(new))
            _state.value = SettingsState.Success("Пароль изменён")
        }
    }

    // === Экспорт зашифрованный (.svault) ===
    fun exportEncrypted(uri: Uri, pwd: String) {
        if (pwd.length < 4) { _state.value = SettingsState.Error("Минимум 4 символа"); return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching {
                val arr = buildJsonArray(repo.getAllSync())
                val encrypted = exportMgr.encrypt(arr.toString(), pwd)
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(encrypted.toByteArray()) }
                _state.value = SettingsState.Success("Экспортировано ${arr.length()} записей ✓")
            }.onFailure { _state.value = SettingsState.Error("Ошибка: ${it.message}") }
        }
    }

    // === Экспорт CSV (незашифрованный — предупредить пользователя) ===
    fun exportCsv(uri: Uri, selectedIds: Set<Long> = emptySet()) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching {
                val all = repo.getAllSync()
                val list = if (selectedIds.isEmpty()) all else all.filter { it.id in selectedIds }
                val sb = StringBuilder()
                sb.appendLine("Название,Профиль,Категория,Логин,Пароль,URL,Заметки,Дата создания")
                list.forEach { e ->
                    sb.appendLine("\"${e.title}\",\"${e.profile}\",\"${e.category}\"," +
                        "\"${e.username}\",\"${e.password}\",\"${e.url}\",\"${e.notes}\"," +
                        "\"${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(e.createdAt)}\"")
                }
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
                _state.value = SettingsState.Success("CSV: экспортировано ${list.size} записей ✓")
            }.onFailure { _state.value = SettingsState.Error("Ошибка: ${it.message}") }
        }
    }

    // === Экспорт TXT ===
    fun exportTxt(uri: Uri, selectedIds: Set<Long> = emptySet()) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching {
                val all = repo.getAllSync()
                val list = if (selectedIds.isEmpty()) all else all.filter { it.id in selectedIds }
                val sb = StringBuilder()
                sb.appendLine("=== SECURE VAULT BACKUP ===")
                sb.appendLine("Дата: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(System.currentTimeMillis())}")
                sb.appendLine("Записей: ${list.size}")
                sb.appendLine()
                list.forEachIndexed { i, e ->
                    sb.appendLine("--- ${i+1}. ${e.title} ---")
                    sb.appendLine("Профиль:   ${e.profile}")
                    sb.appendLine("Категория: ${e.category}")
                    if (e.username.isNotEmpty()) sb.appendLine("Логин:     ${e.username}")
                    sb.appendLine("Пароль:    ${e.password}")
                    if (e.url.isNotEmpty()) sb.appendLine("URL:       ${e.url}")
                    if (e.notes.isNotEmpty()) sb.appendLine("Заметки:   ${e.notes}")
                    if (e.hintText.isNotEmpty()) sb.appendLine("Подсказка: ${e.hintText}")
                    sb.appendLine()
                }
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
                _state.value = SettingsState.Success("TXT: экспортировано ${list.size} записей ✓")
            }.onFailure { _state.value = SettingsState.Error("Ошибка: ${it.message}") }
        }
    }

    // === Импорт (.svault) ===
    fun import(uri: Uri, pwd: String) {
        if (pwd.isBlank()) { _state.value = SettingsState.Error("Введите пароль файла"); return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching {
                val raw = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Не удалось прочитать файл")
                val json = exportMgr.decrypt(raw, pwd) ?: error("Неверный пароль или повреждённый файл")
                val arr = JSONArray(json)
                var count = 0
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    repo.save(0, o.getString("t"), o.optString("pr","Личное"), o.getString("c"),
                        o.getString("u"), o.getString("p"), o.optString("url",""), o.optString("n",""),
                        o.optString("ht",""), o.optString("hk",""), o.optInt("ci",0),
                        o.optBoolean("f",false))
                    count++
                }
                _state.value = SettingsState.Success("Импортировано $count записей ✓")
            }.onFailure { _state.value = SettingsState.Error("Ошибка: ${it.message}") }
        }
    }

    // === Массовая смена паролей ===
    fun massRotatePasswords(profile: String? = null, category: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching {
                val all = repo.getAllSync()
                val targets = all.filter { e ->
                    (profile == null || e.profile == profile) &&
                    (category == null || e.category == category)
                }
                val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*"
                targets.forEach { e ->
                    val len = e.genLength.coerceIn(12, 40)
                    val newPwd = (1..len).map { chars.random() }.joinToString("")
                    repo.updatePasswordOnly(e.id, newPwd)
                }
                _state.value = SettingsState.Success("Обновлено ${targets.size} паролей ✓")
            }.onFailure { _state.value = SettingsState.Error("Ошибка: ${it.message}") }
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState.Loading
            runCatching { repo.deleteAll(); crypto.deleteKey(); session.reset()
                _state.value = SettingsState.Success("RESET") }
                .onFailure { _state.value = SettingsState.Error("Ошибка сброса") }
        }
    }

    fun clearState() { _state.value = SettingsState.Idle }

    private fun buildJsonArray(all: List<Entry>): JSONArray {
        val arr = JSONArray()
        all.forEach { e ->
            arr.put(JSONObject().apply {
                put("t", e.title); put("pr", e.profile); put("c", e.category)
                put("u", e.username); put("p", e.password); put("url", e.url)
                put("n", e.notes); put("ht", e.hintText); put("hk", e.hintKeywords)
                put("ci", e.changeIntervalDays); put("f", e.isFavorite)
            })
        }
        return arr
    }
}
