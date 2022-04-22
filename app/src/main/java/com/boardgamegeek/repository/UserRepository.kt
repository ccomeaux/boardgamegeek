package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntities
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.service.SyncService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepository(val application: BggApplication) {
    private val userDao = UserDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application) }

    suspend fun load(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.loadUser(username)
    }

    suspend fun refresh(username: String): UserEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().user(username)
        val user = response.mapToEntity()
        userDao.saveUser(user)
    }

    suspend fun refreshCollection(username: String, status: String): List<CollectionItemEntity> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<CollectionItemEntity>()
            val response = Adapter.createForXml().collectionC(
                username, mapOf(
                    status to "1",
                    BggService.COLLECTION_QUERY_KEY_BRIEF to "1"
                )
            )
            response.items?.forEach {
                items += it.mapToEntities().first
            }
            items
        }

    suspend fun loadBuddies(sortBy: UserDao.UsersSortBy = UserDao.UsersSortBy.USERNAME): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.loadBuddies(sortBy)
    }

    suspend fun loadAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.loadBuddies(buddiesOnly = false)
    }

    suspend fun refreshBuddies(timestamp: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val accountName = Authenticator.getAccount(application)?.name
        if (accountName.isNullOrBlank()) return@withContext 0 to 0

        val response = Adapter.createForXml().user(accountName, 1, 1)
        val upsertedCount = response.buddies?.buddies?.size ?: 0
        response.buddies?.buddies.orEmpty()
            .map { it.mapToEntity(timestamp) }
            .filter { it.id != INVALID_ID && it.userName.isNotBlank() }
            .forEach {
                userDao.saveBuddy(it)
            }

        val deletedCount = userDao.deleteUsersAsOf(timestamp)
        Timber.d("Deleted $deletedCount users")

        upsertedCount to deletedCount
    }

    suspend fun updateNickName(username: String, nickName: String) {
        if (username.isNotBlank()) {
            userDao.upsert(contentValuesOf(Buddies.Columns.PLAY_NICKNAME to nickName), username)
        }
    }

    fun updateSelf(user: UserEntity?) {
        Authenticator.putUserId(application, user?.id ?: INVALID_ID)
        if (!user?.userName.isNullOrEmpty()) FirebaseCrashlytics.getInstance().setUserId(user?.userName.hashCode().toString())
        prefs[AccountPreferences.KEY_USERNAME] = user?.userName.orEmpty()
        prefs[AccountPreferences.KEY_FULL_NAME] = user?.fullName.orEmpty()
        prefs[AccountPreferences.KEY_AVATAR_URL] = user?.avatarUrl.orEmpty()
    }

    suspend fun deleteUsers(): Int {
        syncPrefs.clearBuddyListTimestamps()
        val count = userDao.deleteUsers()
        Timber.i("Removed %d users", count)
        return count
    }

    suspend fun resetUsers() {
        deleteUsers()
        //TODO remove buddy colors
        SyncService.sync(application, SyncService.FLAG_SYNC_BUDDIES)
    }
}
