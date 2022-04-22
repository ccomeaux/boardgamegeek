package com.boardgamegeek.entities

import com.boardgamegeek.provider.BggContract

data class UserEntity(
    val internalId: Long,
    val id: Int,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val avatarUrlRaw: String,
    val playNickname: String = "",
    val updatedTimestamp: Long = 0L,
) {
    val fullName = "$firstName $lastName".trim()

    val description = if (userName.isBlank()) fullName else "$fullName ($userName)"

    val avatarUrl: String = avatarUrlRaw
        get() = if (field == BggContract.INVALID_URL) "" else field

    fun generateSyncHashCode(): Int {
        return ("${firstName}\n${lastName}\n${avatarUrl}\n").hashCode()
    }
}
