package com.bilocan.sifreyoneticisi.data

import androidx.room.*
import com.bilocan.sifreyoneticisi.model.AppPassword

@Dao
interface AppPasswordDao {
    @Query("SELECT * FROM app_password WHERE id = 1")
    suspend fun getAppPassword(): AppPassword?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppPassword(appPassword: AppPassword)

    @Query("SELECT EXISTS(SELECT 1 FROM app_password WHERE id = 1)")
    suspend fun hasPassword(): Boolean
} 