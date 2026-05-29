package com.securevault.data.repository

import com.securevault.data.db.VaultDao
import com.securevault.data.model.VaultEntry
import com.securevault.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Entry(
    val id: Long, val title: String,
    val profile: String, val category: String,
    val username: String, val password: String,
    val url: String, val notes: String,
    val hintText: String, val hintKeywords: String,
    val passwordChangedAt: Long, val changeIntervalDays: Int,
    val genLength: Int, val genUpper: Boolean, val genDigits: Boolean, val genSymbols: Boolean,
    val createdAt: Long, val updatedAt: Long, val isFavorite: Boolean
) {
    val isPasswordExpired: Boolean
        get() = changeIntervalDays > 0 &&
            (System.currentTimeMillis() - passwordChangedAt) / 86_400_000L >= changeIntervalDays
    val daysUntilExpiry: Long
        get() = if (changeIntervalDays == 0) Long.MAX_VALUE
                else changeIntervalDays - (System.currentTimeMillis() - passwordChangedAt) / 86_400_000L
}

@Singleton
class VaultRepository @Inject constructor(
    private val dao: VaultDao,
    private val crypto: CryptoManager
) {
    fun getAll(): Flow<List<Entry>> = dao.getAll().map { it.map(::dec) }
    fun getByProfile(p: String): Flow<List<Entry>> = dao.getByProfile(p).map { it.map(::dec) }
    fun getFavorites(): Flow<List<Entry>> = dao.getFavorites().map { it.map(::dec) }
    fun search(q: String): Flow<List<Entry>> = dao.search(q).map { it.map(::dec) }
    fun getCategories(): Flow<List<String>> = dao.getCategories()
    fun getProfiles(): Flow<List<String>> = dao.getProfiles()
    fun getCount(): Flow<Int> = dao.getCount()
    fun getExpired(): Flow<List<Entry>> = dao.getExpiredPasswords(System.currentTimeMillis()).map { it.map(::dec) }

    suspend fun getAllSync(): List<Entry> = dao.getAll().first().map(::dec)
    suspend fun getById(id: Long): Entry? = dao.getById(id)?.let(::dec)

    suspend fun save(
        id: Long = 0, title: String, profile: String = "Личное", category: String,
        username: String, password: String, url: String = "", notes: String = "",
        hintText: String = "", hintKeywords: String = "",
        changeIntervalDays: Int = 0, isFavorite: Boolean = false,
        genLength: Int = 20, genUpper: Boolean = true, genDigits: Boolean = true, genSymbols: Boolean = true
    ): Long {
        val existing = if (id > 0) dao.getById(id) else null
        val changedAt = if (existing != null) {
            val oldPwd = try { crypto.decryptString(existing.encryptedPassword) } catch (_: Exception) { "" }
            if (oldPwd != password) System.currentTimeMillis() else existing.passwordChangedAt
        } else System.currentTimeMillis()

        return dao.insert(VaultEntry(
            id = id, title = title, profile = profile, category = category,
            encryptedUsername = enc(username), encryptedPassword = enc(password),
            encryptedUrl = enc(url), encryptedNotes = enc(notes),
            hintText = hintText, hintKeywords = hintKeywords,
            passwordChangedAt = changedAt, changeIntervalDays = changeIntervalDays,
            genLength = genLength, genUpper = genUpper, genDigits = genDigits, genSymbols = genSymbols,
            updatedAt = System.currentTimeMillis(), isFavorite = isFavorite
        ))
    }

    suspend fun updatePasswordOnly(id: Long, newPassword: String) {
        dao.getById(id)?.let {
            dao.update(it.copy(
                encryptedPassword = enc(newPassword),
                passwordChangedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun toggleFavorite(e: Entry) {
        dao.getById(e.id)?.let { dao.update(it.copy(isFavorite = !it.isFavorite)) }
    }
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()

    private fun enc(s: String) = if (s.isNotEmpty()) crypto.encryptString(s) else ""
    private fun dec(s: String) = if (s.isNotEmpty()) try { crypto.decryptString(s) } catch (_: Exception) { "" } else ""
    private fun dec(e: VaultEntry) = Entry(
        e.id, e.title, e.profile, e.category,
        dec(e.encryptedUsername), dec(e.encryptedPassword),
        dec(e.encryptedUrl), dec(e.encryptedNotes),
        e.hintText, e.hintKeywords,
        e.passwordChangedAt, e.changeIntervalDays,
        e.genLength, e.genUpper, e.genDigits, e.genSymbols,
        e.createdAt, e.updatedAt, e.isFavorite
    )
}
