package com.bilocan.sifreyoneticisi.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val icon: Int,  // Drawable resource ID
    val color: Int, // Color resource ID
    val orderIndex: Int = 0,
    var passwordCount: Int = 0
) 