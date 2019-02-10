package com.boardgamegeek.repository

import android.content.ContentProviderOperation
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.RateLimiter
import hugo.weaving.DebugLog
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PlayRepository(val application: BggApplication) {
    private val playDao = PlayDao(application)
    private val gameDao = GameDao(application)
    private val collectionDao = CollectionDao(application)
    private val playsRateLimiter = RateLimiter<Int>(10, TimeUnit.MINUTES)
    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    fun getPlays(): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : RefreshableResourceLoader<List<PlayEntity>, PlaysResponse>(application) {
            val persister = PlayPersister(application)
            var timestamp = 0L
            val newestTimestamp: Long? = SyncPrefs.getPlaysNewestTimestamp(application)
            val oldestTimestamp = SyncPrefs.getPlaysOldestTimestamp(application)
            val mapper = PlayMapper()
            var sortingNewest = false
            var lastNewPage = 0

            override val typeDescriptionResId: Int
                get() = R.string.title_plays

            @DebugLog
            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlays()
            }

            @DebugLog
            override fun shouldRefresh(data: List<PlayEntity>?): Boolean {
                if (!PreferencesUtils.getSyncPlays(application)) return false
                return data == null || data.isEmpty() || playsRateLimiter.shouldProcess(0)
            }

            @DebugLog
            override fun createCall(page: Int): Call<PlaysResponse> {
                if (page == 1) {
                    timestamp = System.currentTimeMillis()
                    sortingNewest = true
                }
                if (sortingNewest) lastNewPage = page
                return if (sortingNewest) {
                    Adapter.createForXml().plays(username,
                            newestTimestamp?.asDateForApi(),
                            null,
                            page)
                } else {
                    Adapter.createForXml().plays(username,
                            null,
                            oldestTimestamp.asDateForApi(),
                            page - lastNewPage)
                }
            }

            @DebugLog
            override fun saveCallResult(result: PlaysResponse) {
                val plays = mapper.map(result.plays)
                persister.save(plays, timestamp)
                updateTimestamps(plays)
                Timber.i("Synced page %,d of plays", 1)
            }

            @DebugLog
            override fun hasMorePages(result: PlaysResponse): Boolean {
                return when {
                    result.hasMorePages() -> true
                    sortingNewest -> {
                        sortingNewest = false
                        oldestTimestamp > 0L
                    }
                    else -> false
                }
            }

            override fun onRefreshSucceeded() {
                newestTimestamp?.let { playDao.deleteUnupdatedPlaysSince(timestamp, it) }
                if (oldestTimestamp > 0L) {
                    playDao.deleteUnupdatedPlaysBefore(timestamp, oldestTimestamp)
                } else {
                    SyncPrefs.setPlaysOldestTimestamp(application, 0L)
                }
                CalculatePlayStatsTask(application).executeAsyncTask()
            }

            override fun onRefreshFailed() {
                playsRateLimiter.reset(0)
            }

            override fun onRefreshCancelled() {
                playsRateLimiter.reset(0)
            }

            @DebugLog
            private fun updateTimestamps(plays: List<Play>?) {
                val newestDate = plays?.maxBy { it.dateInMillis }?.dateInMillis ?: 0L
                if (newestDate > SyncPrefs.getPlaysNewestTimestamp(application) ?: 0L) {
                    SyncPrefs.setPlaysNewestTimestamp(application, newestDate)
                }
                val oldestDate = plays?.minBy { it.dateInMillis }?.dateInMillis ?: Long.MAX_VALUE
                if (oldestDate < SyncPrefs.getPlaysOldestTimestamp(application)) {
                    SyncPrefs.setPlaysOldestTimestamp(application, oldestDate)
                }
            }
        }.asLiveData()
    }

    fun loadForStatsAsLiveData(): LiveData<List<GameForPlayStatEntity>> {
        // TODO use PlayDao if either of these is false
        // val isOwnedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_OWN)
        // val isPlayedSynced = PreferencesUtils.isStatusSetToSync(application, BggService.COLLECTION_QUERY_STATUS_PLAYED)

        return Transformations.map(gameDao.loadPlayInfoAsLiveData(
                PreferencesUtils.logPlayStatsIncomplete(application),
                PreferencesUtils.logPlayStatsExpansions(application),
                PreferencesUtils.logPlayStatsAccessories(application)))
        {
            return@map filterGamesOwned(it)
        }
    }

    fun loadForStats(): List<GameForPlayStatEntity> {
        val playInfo = gameDao.loadPlayInfo(PreferencesUtils.logPlayStatsIncomplete(application),
                PreferencesUtils.logPlayStatsExpansions(application),
                PreferencesUtils.logPlayStatsAccessories(application))
        return filterGamesOwned(playInfo)
    }

    private fun filterGamesOwned(playInfo: List<GameForPlayStatEntity>): List<GameForPlayStatEntity> {
        val items = collectionDao.load()
        val games = mutableListOf<GameForPlayStatEntity>()
        playInfo.forEach { game ->
            games += game.copy(isOwned = items.any { item -> item.gameId == game.id && item.own })
        }
        return games.toList()
    }

    fun loadPlayers(sortBy: PlayDao.PlayerSortBy = PlayDao.PlayerSortBy.NAME): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayersAsLiveData(sortBy)
    }

    fun loadPlayersForStats(): List<PlayerEntity> {
        return playDao.loadPlayers(PreferencesUtils.logPlayStatsIncomplete(application))
    }

    fun loadPlayersForStatsAsLiveData(): LiveData<List<PlayerEntity>> {
        return playDao.loadPlayersAsLiveData(PreferencesUtils.logPlayStatsIncomplete(application))
    }

    fun loadUserPlayer(username: String): LiveData<PlayerEntity> {
        return playDao.loadUserPlayerAsLiveData(username)
    }

    fun loadNonUserPlayer(playerName: String): LiveData<PlayerEntity> {
        return playDao.loadNonUserPlayerAsLiveData(playerName)
    }

    fun loadUserColorsAsLiveData(username: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadUserColorsAsLiveData(username)
    }

    fun loadUserColors(username: String): List<PlayerColorEntity> {
        return playDao.loadUserColors(username)
    }

    fun loadPlayerColorsAsLiveData(playerName: String): LiveData<List<PlayerColorEntity>> {
        return playDao.loadPlayerColorsAsLiveData(playerName)
    }

    fun loadPlayerColors(playerName: String): List<PlayerColorEntity> {
        return playDao.loadPlayerColors(playerName)
    }

    fun savePlayerColors(playerName: String, colors: List<PlayerColorEntity>?) {
        playDao.savePlayerColors(playerName, colors)
    }

    fun saveUserColors(username: String, colors: List<PlayerColorEntity>?) {
        playDao.saveUserColors(username, colors)
    }

    fun loadLocations(sortBy: PlayDao.LocationSortBy = PlayDao.LocationSortBy.NAME): LiveData<List<LocationEntity>> {
        return playDao.loadLocationsAsLiveData(sortBy)
    }

    fun updatePlaysWithNickName(username: String, nickName: String): Int {
        val count = playDao.countNickNameUpdatePlays(username, nickName)
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForUserAndNickNameOperations(username, nickName)
        batch += playDao.createNickNameUpdateOperation(username, nickName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(application, batch)
        }
        return count
    }

    fun renamePlayer(oldName: String, newName: String) {
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(oldName)
        batch += playDao.createRenameUpdateOperation(oldName, newName)
        batch += playDao.createCopyPlayerColorsOperations(oldName, newName)
        batch += playDao.createDeletePlayerColorsOperation(oldName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(application, batch)
        }
    }

    fun addUsernameToPlayer(playerName: String, username: String) {
        // TODO verify username is good
        val batch = arrayListOf<ContentProviderOperation>()
        batch += playDao.createDirtyPlaysForNonUserPlayerOperations(playerName)
        batch += playDao.createAddUsernameOperation(playerName, username)
        batch += playDao.createCopyPlayerColorsToUserOperations(playerName, username)
        batch += playDao.createDeletePlayerColorsOperation(playerName)
        application.appExecutors.diskIO.execute {
            application.contentResolver.applyBatch(application, batch)
        }
    }

    fun updateGameHIndex(hIndex: Int) {
        PreferencesUtils.updateGameHIndex(application, hIndex)
    }

    fun updatePlayerHIndex(hIndex: Int) {
        PreferencesUtils.updatePlayerHIndex(application, hIndex)
    }
}
