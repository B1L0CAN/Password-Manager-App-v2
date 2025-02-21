package com.bilocan.sifreyoneticisi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bilocan.sifreyoneticisi.model.Category
import com.bilocan.sifreyoneticisi.model.Password
import com.bilocan.sifreyoneticisi.model.AppPassword

private const val DATABASE_VERSION = 4
private const val DATABASE_NAME = "password_manager_db"

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Önce yeni tablolar oluştur
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                icon INTEGER NOT NULL,
                color INTEGER NOT NULL,
                orderIndex INTEGER NOT NULL DEFAULT 0
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS passwords_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                notes TEXT
            )
        """)

        // Eski verileri yeni tablolara aktar
        database.execSQL("""
            INSERT INTO categories_new (id, name, icon, color, orderIndex)
            SELECT id, name, icon, color, orderIndex FROM categories
        """)

        database.execSQL("""
            INSERT INTO passwords_new (id, title, username, password, categoryId, notes)
            SELECT id, appName, COALESCE(username, ''), password, categoryId, NULL FROM passwords
        """)

        // Eski tabloları sil
        database.execSQL("DROP TABLE IF EXISTS categories")
        database.execSQL("DROP TABLE IF EXISTS passwords")

        // Yeni tabloları yeniden adlandır
        database.execSQL("ALTER TABLE categories_new RENAME TO categories")
        database.execSQL("ALTER TABLE passwords_new RENAME TO passwords")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Önce yeni tablo oluştur
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS passwords_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                orderIndex INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Eski verileri yeni tabloya aktar
        database.execSQL("""
            INSERT INTO passwords_new (id, title, username, password, categoryId, orderIndex)
            SELECT id, title, username, password, categoryId, 0 FROM passwords
        """)

        // Eski tabloyu sil
        database.execSQL("DROP TABLE IF EXISTS passwords")

        // Yeni tabloyu yeniden adlandır
        database.execSQL("ALTER TABLE passwords_new RENAME TO passwords")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Uygulama şifresi tablosunu oluştur
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS app_password (
                id INTEGER PRIMARY KEY NOT NULL,
                password TEXT NOT NULL
            )
        """)
    }
}

@Database(
    entities = [Category::class, Password::class, AppPassword::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun passwordDao(): PasswordDao
    abstract fun appPasswordDao(): AppPasswordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
} 