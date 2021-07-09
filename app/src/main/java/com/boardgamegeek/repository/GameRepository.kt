package com.boardgamegeek.repository

import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.ImageUtils.getImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameRepository(val application: BggApplication) {
    private val dao = GameDao(application)
    private val playDao = PlayDao(application)
    private val bggService = Adapter.createForXml()
    private val mapper = PlayMapper()
    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    suspend fun loadGame(gameId: Int) = dao.load(gameId)

    suspend fun refreshGame(gameId: Int): GameEntity? = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = bggService.thing2(gameId, 1)
        for (game in response.games) {
            dao.save(game.mapToEntity(), timestamp)
            Timber.i("Synced game '$gameId'")
        }
        response.games.firstOrNull()?.mapToEntity()
    }

    suspend fun refreshHeroImage(game: GameEntity): GameEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image2(game.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(game.id, contentValuesOf(BggContract.Games.HERO_IMAGE_URL to url))
        game.copy(heroImageUrl = url)
    }

    suspend fun getRanks(gameId: Int) = dao.loadRanks(gameId)

    suspend fun getLanguagePoll(gameId: Int) = dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)

    suspend fun getAgePoll(gameId: Int) = dao.loadPoll(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE)

    suspend fun getPlayerPoll(gameId: Int) = dao.loadPlayerPoll(gameId)

    suspend fun getDesigners(gameId: Int) = dao.loadDesigners(gameId)

    suspend fun getArtists(gameId: Int) = dao.loadArtists(gameId)

    suspend fun getPublishers(gameId: Int) = dao.loadPublishers(gameId)

    suspend fun getCategories(gameId: Int) = dao.loadCategories(gameId)

    suspend fun getMechanics(gameId: Int) = dao.loadMechanics(gameId)

    suspend fun getExpansions(gameId: Int) = dao.loadExpansions(gameId)

    suspend fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true)

    suspend fun refreshPlays(gameId: Int): List<PlayEntity> = withContext(Dispatchers.IO) {
        val plays = mutableListOf<PlayEntity>()
        val timestamp = System.currentTimeMillis()
        var page = 1
        do {
            val response = bggService.playsByGameC(username, gameId, page++)
            val playsPage = mapper.map(response.plays, timestamp)
            playDao.save(playsPage, timestamp)
            plays += playsPage
        } while (response.hasMorePages())

        playDao.deleteUnupdatedPlays(gameId, timestamp)
        dao.updateC(gameId, contentValuesOf(BggContract.Games.UPDATED_PLAYS to System.currentTimeMillis()))

        CalculatePlayStatsTask(application).executeAsyncTask() // TODO replace with coroutine

        plays
    }

    suspend fun refreshPartialPlays(gameId: Int) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = bggService.playsByGameC(username, gameId, 1)
        val plays = mapper.map(response.plays, timestamp)
        playDao.save(plays, timestamp)
        CalculatePlayStatsTask(application).executeAsyncTask()
    }

    suspend fun getPlays(gameId: Int) = playDao.loadPlaysByGame(gameId)

    suspend fun getPlayColors(gameId: Int) = dao.loadPlayColors(gameId)

    suspend fun getColors(gameId: Int) = dao.loadColors(gameId)

    suspend fun addPlayColor(gameId: Int, color: String?) {
        if (gameId != BggContract.INVALID_ID && !color.isNullOrBlank()) {
            dao.insertColor(gameId, color)
        }
    }

    suspend fun deletePlayColor(gameId: Int, color: String): Int {
        return if (gameId != BggContract.INVALID_ID && color.isNotBlank()) {
            dao.deleteColor(gameId, color)
        } else 0
    }

    suspend fun computePlayColors(gameId: Int) = withContext(Dispatchers.IO) {
        dao.computeColors(gameId)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            if (gameId != BggContract.INVALID_ID) {
                dao.updateC(gameId, contentValuesOf(BggContract.Games.LAST_VIEWED to lastViewed))
            }
        }

    suspend fun updateGameColors(
        gameId: Int,
        iconColor: Int,
        darkColor: Int,
        winsColor: Int,
        winnablePlaysColor: Int,
        allPlaysColor: Int
    ) {
        if (gameId != BggContract.INVALID_ID) {
            val values = contentValuesOf(
                BggContract.Games.ICON_COLOR to iconColor,
                BggContract.Games.DARK_COLOR to darkColor,
                BggContract.Games.WINS_COLOR to winsColor,
                BggContract.Games.WINNABLE_PLAYS_COLOR to winnablePlaysColor,
                BggContract.Games.ALL_PLAYS_COLOR to allPlaysColor,
            )
            val numberOfRowsModified = dao.updateC(gameId, values)
            Timber.d(numberOfRowsModified.toString())
        }
    }

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != BggContract.INVALID_ID) {
            dao.updateC(gameId, contentValuesOf(BggContract.Games.STARRED to if (isFavorite) 1 else 0))
        }
    }
}
