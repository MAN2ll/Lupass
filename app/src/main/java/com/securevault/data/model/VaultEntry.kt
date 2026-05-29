package com.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val profile: String = "Личное",           // "Личное" | "Рабочее" | "Другое"
    val category: String = "Общее",
    val encryptedUsername: String = "",
    val encryptedPassword: String = "",
    val encryptedUrl: String = "",
    val encryptedNotes: String = "",
    val hintText: String = "",                  // пользовательская подсказка текстом
    val hintKeywords: String = "",              // ключевые слова через запятую → эмодзи
    val passwordChangedAt: Long = System.currentTimeMillis(),
    val changeIntervalDays: Int = 0,            // 0=без напоминания, 30/60/90/180/365
    val genLength: Int = 20,                    // настройки генератора для этой записи
    val genUpper: Boolean = true,
    val genDigits: Boolean = true,
    val genSymbols: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
