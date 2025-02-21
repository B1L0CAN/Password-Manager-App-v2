package com.bilocan.sifreyoneticisi.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_password")
data class AppPassword(
    @PrimaryKey
    val id: Int = 1,  // Her zaman tek bir kayıt olacak
    val password: String
) 