package com.bilocan.sifreyoneticisi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun getUser(username: String, password: String): User?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): User?

    @Query("UPDATE users SET username = :username, password = :password WHERE id = :userId")
    suspend fun updateUser(userId: Int, username: String, password: String)
} 