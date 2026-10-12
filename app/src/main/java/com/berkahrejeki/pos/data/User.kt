package com.berkahrejeki.pos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { ADMIN, KASIR }

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val role: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Session(
    val userId: Long,
    val username: String,
    val fullName: String,
    val role: UserRole
) {
    val isAdmin get() = role == UserRole.ADMIN
}
