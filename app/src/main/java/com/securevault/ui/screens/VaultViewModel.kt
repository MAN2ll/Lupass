package com.securevault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.data.repository.Entry
import com.securevault.data.repository.VaultRepository
import com.securevault.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repo: VaultRepository,
    private val session: SessionManager
) : ViewModel() {
    private val _query    = MutableStateFlow("")
    private val _category = MutableStateFlow<String?>(null)
    private val _profile  = MutableStateFlow<String?>(null)  // null = Все профили
    private val _showExpiredOnly = MutableStateFlow(false)

    val query:    StateFlow<String>  = _query
    val category: StateFlow<String?> = _category
    val profile:  StateFlow<String?> = _profile
    val showExpiredOnly: StateFlow<Boolean> = _showExpiredOnly

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawEntries: StateFlow<List<Entry>> = _query
        .flatMapLatest { q -> if (q.isEmpty()) repo.getAll() else repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<Entry>> = combine(rawEntries, _category, _profile, _showExpiredOnly)
    { all, cat, prof, expOnly ->
        all.filter { e ->
            (cat == null || e.category == cat) &&
            (prof == null || e.profile == prof) &&
            (!expOnly || e.isPasswordExpired)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Entry>> = repo.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expired: StateFlow<List<Entry>> = repo.getExpired()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repo.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<String>> = repo.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> = repo.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredCount: StateFlow<Int> = expired
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String?) { _category.value = c }
    fun setProfile(p: String?) { _profile.value = p }
    fun setShowExpiredOnly(v: Boolean) { _showExpiredOnly.value = v }
    fun toggleFavorite(e: Entry) { viewModelScope.launch { repo.toggleFavorite(e) } }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
    fun lock() { session.lock() }
}
