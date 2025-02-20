package com.bilocan.sifreyoneticisi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bilocan.sifreyoneticisi.model.Category
import com.bilocan.sifreyoneticisi.model.Password
import com.bilocan.sifreyoneticisi.data.User

private const val DATABASE_VERSION = 3
private const val DATABASE_NAME = "app_database"

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS passwords_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                appName TEXT NOT NULL,
                username TEXT,
                password TEXT NOT NULL
            )
        """)
        
        database.execSQL("""
            INSERT OR IGNORE INTO passwords_new (id, categoryId, appName, password)
            SELECT id, categoryId, appName, password FROM passwords
        """)
        
        database.execSQL("DROP TABLE IF EXISTS passwords")
        database.execSQL("ALTER TABLE passwords_new RENAME TO passwords")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Passwords tablosuna orderIndex ekle
        database.execSQL("ALTER TABLE passwords ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        // Categories tablosuna orderIndex ekle
        database.execSQL("ALTER TABLE categories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        
        // Mevcut kayıtların sırasını ID'lerine göre ayarla
        database.execSQL("UPDATE passwords SET orderIndex = id - 1")
        database.execSQL("UPDATE categories SET orderIndex = id - 1")
    }
}

@Database(
    entities = [User::class, Password::class, Category::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun passwordDao(): PasswordDao
    abstract fun categoryDao(): CategoryDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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