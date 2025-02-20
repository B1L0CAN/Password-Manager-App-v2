package com.bilocan.sifreyoneticisi.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val icon: Int,  // Drawable resource ID
    val color: Int, // Color resource ID
    var passwordCount: Int = 0,
    var orderIndex: Int = 0
) 