package com.securevault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.data.repository.Entry
import com.securevault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(private val repo: VaultRepository) : ViewModel() {
    private val _entry = MutableStateFlow<Entry?>(null)
    val entry: StateFlow<Entry?> = _entry
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun load(id: Long) { viewModelScope.launch { _entry.value = repo.getById(id) } }

    fun save(
        id: Long, title: String, profile: String, category: String,
        username: String, password: String, url: String, notes: String,
        hintText: String, hintKeywords: String, changeIntervalDays: Int, isFavorite: Boolean,
        genLength: Int, genUpper: Boolean, genDigits: Boolean, genSymbols: Boolean
    ) {
        if (title.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            repo.save(id, title, profile, category, username, password, url, notes,
                hintText, hintKeywords, changeIntervalDays, isFavorite,
                genLength, genUpper, genDigits, genSymbols)
            _saved.value = true
        }
    }

    fun generatePassword(len: Int = 20, upper: Boolean = true, digits: Boolean = true, symbols: Boolean = true): String {
        var chars = "abcdefghijklmnopqrstuvwxyz"
        if (upper)   chars += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (digits)  chars += "0123456789"
        if (symbols) chars += "!@#\$%^&*()-_=+[]{}|;:,.<>?"
        return (1..len).map { chars.random() }.joinToString("")
    }
}
