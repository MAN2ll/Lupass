package com.securevault.data.db

import androidx.room.*
import com.securevault.data.model.VaultEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE profile = :profile ORDER BY updatedAt DESC")
    fun getByProfile(profile: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE title LIKE '%' || :q || '%' OR encryptedUsername LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntry?

    @Query("SELECT * FROM vault_entries WHERE changeIntervalDays > 0 AND ((:now - passwordChangedAt) / 86400000) >= changeIntervalDays ORDER BY passwordChangedAt ASC")
    fun getExpiredPasswords(now: Long): Flow<List<VaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: VaultEntry): Long

    @Update
    suspend fun update(e: VaultEntry)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT category FROM vault_entries ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT profile FROM vault_entries ORDER BY profile")
    fun getProfiles(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM vault_entries")
    fun getCount(): Flow<Int>
}
