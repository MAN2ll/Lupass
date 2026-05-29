package com.securevault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.securevault.data.model.VaultEntry

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN profile TEXT NOT NULL DEFAULT 'Личное'")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN hintText TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN hintKeywords TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN passwordChangedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN changeIntervalDays INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN genLength INTEGER NOT NULL DEFAULT 20")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN genUpper INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN genDigits INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE vault_entries ADD COLUMN genSymbols INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(entities = [VaultEntry::class], version = 2, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun dao(): VaultDao
}
