package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.PreferencesUtils
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UserRepository(val application: BggApplication) {
    private val userDao = UserDao(application)

    fun loadUser(username: String): LiveData<RefreshableResource<UserEntity>> {
        return object : RefreshableResourceLoader<UserEntity, User>(application) {
            override fun loadFromDatabase(): LiveData<UserEntity> {
                return userDao.loadUserAsLiveData(username)
            }

            override fun shouldRefresh(data: UserEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_buddy

            override fun createCall(page: Int): Call<User> {
                return Adapter.createForXml().user(username)
            }

            override fun saveCallResult(result: User) {
                userDao.saveUser(result)
            }
        }.asLiveData()
    }

    fun loadBuddies(sortBy: UserDao.UsersSortBy = UserDao.UsersSortBy.USERNAME): LiveData<RefreshableResource<List<UserEntity>>> {
        return object : RefreshableResourceLoader<List<UserEntity>, User>(application) {
            private var timestamp = 0L
            private var accountName: String? = null

            override fun loadFromDatabase(): LiveData<List<UserEntity>> {
                return userDao.loadBuddiesAsLiveData(sortBy)
            }

            override fun shouldRefresh(data: List<UserEntity>?): Boolean {
                if (!PreferencesUtils.getSyncBuddies(application)) return false
                accountName = Authenticator.getAccount(application)?.name
                if (accountName == null) return false
                if (data == null) return true
                val lastCompleteSync = SyncPrefs.getBuddiesTimestamp(application)
                return lastCompleteSync.isOlderThan(1, TimeUnit.HOURS)
            }

            override val typeDescriptionResId = R.string.title_buddies

            override fun createCall(page: Int): Call<User> {
                timestamp = System.currentTimeMillis()
                return Adapter.createForXml().user(accountName, 1, page)
            }

            override fun saveCallResult(result: User) {
                userDao.saveUser(result.id, result.name, false)
                var upsertedCount = 0
                (result.buddies?.buddies ?: emptyList()).forEach {
                    upsertedCount += userDao.saveUser(it.id.toIntOrNull()
                            ?: BggContract.INVALID_ID, it.name, updateTime = timestamp)
                }
                Timber.d("Upserted $upsertedCount users")
            }

            override fun onRefreshSucceeded() {
                val deletedCount = userDao.deleteUsersAsOf(timestamp)
                Timber.d("Deleted $deletedCount users")
                SyncPrefs.setBuddiesTimestamp(application, timestamp)
            }
        }.asLiveData()
    }

    fun updateNickName(username: String, nickName: String) {
        if (username.isBlank()) return
        application.appExecutors.diskIO.execute {
            userDao.updateNickName(username, nickName)
        }
    }
}