package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.ImageRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.RateLimiter
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val itemsRateLimiter = RateLimiter<Int>(RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES), TimeUnit.MINUTES)
    private val gameRateLimiter = RateLimiter<Int>(RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES), TimeUnit.MINUTES)
    private val partialPlaysRateLimiter = RateLimiter<Int>(RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES), TimeUnit.MINUTES)
    private val fullPlaysRateLimiter = RateLimiter<Int>(RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS), TimeUnit.HOURS)

    private val _gameId = MutableLiveData<Int>()
    val gameId: LiveData<Int>
        get() = _gameId

    private val _producerType = MutableLiveData<ProducerType>()
    val producerType: LiveData<ProducerType>
        get() = _producerType

    enum class ProducerType(val value: Int) {
        UNKNOWN(0),
        DESIGNER(1),
        ARTIST(2),
        PUBLISHER(3),
        CATEGORIES(4),
        MECHANICS(5),
        EXPANSIONS(6),
        BASE_GAMES(7);

        companion object {
            private val map = values().associateBy(ProducerType::value)
            fun fromInt(value: Int?) = map[value] ?: UNKNOWN
        }
    }

    private val gameRepository = GameRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val imageRepository = ImageRepository(getApplication())

    fun setId(gameId: Int) {
        if (_gameId.value != gameId) {
            viewModelScope.launch {
                gameRepository.updateLastViewed(gameId, System.currentTimeMillis())
            }
            _gameId.value = gameId
        }
    }

    fun setProducerType(type: ProducerType) {
        if (_producerType.value != type) _producerType.value = type
    }

    val game: LiveData<RefreshableResource<GameEntity>> = _gameId.switchMap { gameId ->
        liveData {
            try {
                if (gameId == BggContract.INVALID_ID) {
                    emit(RefreshableResource.success(null))
                } else {
                    latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                    val game = gameRepository.loadGame(gameId)
                    emit(RefreshableResource.success(game))
                    val refreshedGame = if (game == null || gameRateLimiter.shouldProcess(gameId, game.updated)) {
                        emit(RefreshableResource.refreshing(game))
                        gameRepository.refreshGame(gameId)
                        val loadedGame = gameRepository.loadGame(gameId)
                        emit(RefreshableResource.success(loadedGame))
                        gameRateLimiter.reset(gameId)
                        loadedGame
                    } else game
                    refreshedGame?.let {
                        if (it.heroImageUrl.isBlank()) {
                            emit(RefreshableResource.refreshing(it))
                            gameRepository.refreshHeroImage(it)
                            val gameWithHeroImage = gameRepository.loadGame(gameId)
                            emit(RefreshableResource.success(gameWithHeroImage))
                        }
                    }
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
                gameRateLimiter.reset(gameId)
            }
        }
    }

    val ranks = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getRanks(it.id)
            })
        }.distinctUntilChanged()
    }

    val languagePoll = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getLanguagePoll(it.id)
            })
        }.distinctUntilChanged()
    }

    val agePoll = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getAgePoll(it.id)
            })
        }.distinctUntilChanged()
    }

    val playerPoll = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getPlayerPoll(it.id)
            })
        }.distinctUntilChanged()
    }

    val designers = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getDesigners(it.id)
            })
        }.distinctUntilChanged()
    }

    val artists = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getArtists(it.id)
            })
        }.distinctUntilChanged()
    }

    val publishers = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getPublishers(it.id)
            })
        }.distinctUntilChanged()
    }

    val categories = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getCategories(it.id)
            })
        }.distinctUntilChanged()
    }

    val mechanics = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getMechanics(it.id)
            })
        }.distinctUntilChanged()
    }

    val expansions = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getExpansions(it.id).map { expansion ->
                    GameDetailEntity(expansion.id, expansion.name, describeStatuses(expansion))
                }
            })
        }.distinctUntilChanged()
    }

    val baseGames = game.switchMap {
        liveData {
            emit(it.data?.let {
                if (it.id == BggContract.INVALID_ID) null else gameRepository.getBaseGames(it.id).map { expansion ->
                    GameDetailEntity(expansion.id, expansion.name, describeStatuses(expansion))
                }
            })
        }.distinctUntilChanged()
    }

    private fun describeStatuses(entity: GameExpansionsEntity): String {
        val ctx = getApplication<BggApplication>()
        val statuses = mutableListOf<String>()
        if (entity.own) statuses.add(ctx.getString(R.string.collection_status_own))
        if (entity.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
        if (entity.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
        if (entity.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
        if (entity.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
        if (entity.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
        if (entity.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
        if (entity.wishList) statuses.add(entity.wishListPriority.asWishListPriority(ctx))
        if (entity.numberOfPlays > 0) statuses.add(ctx.getString(R.string.played))
        if (entity.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
        if (entity.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
        return statuses.formatList()
    }

    val producers = _producerType.switchMap { type ->
        when (type) {
            ProducerType.DESIGNER -> designers
            ProducerType.ARTIST -> artists
            ProducerType.PUBLISHER -> publishers
            ProducerType.CATEGORIES -> categories
            ProducerType.MECHANICS -> mechanics
            ProducerType.EXPANSIONS -> expansions
            ProducerType.BASE_GAMES -> baseGames
            else -> liveData { emit(null) }
        }
    }

    val collectionItems: LiveData<RefreshableResource<List<CollectionItemEntity>>> = game.switchMap { game ->
        liveData {
            val gameId = game.data?.id ?: BggContract.INVALID_ID
            try {
                if (gameId == BggContract.INVALID_ID) {
                    emit(RefreshableResource.success(emptyList()))
                } else {
                    latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                    val items = gameCollectionRepository.loadCollectionItems(gameId)
                    emit(RefreshableResource.success(items))
                    val lastUpdated = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
                    val refreshedItems = if (itemsRateLimiter.shouldProcess(gameId, lastUpdated)) {
                        emit(RefreshableResource.refreshing(items))
                        gameCollectionRepository.refreshCollectionItems(gameId, game.data?.subtype.orEmpty())
                        val newItems = gameCollectionRepository.loadCollectionItems(gameId)
                        emit(RefreshableResource.success(newItems))
                        itemsRateLimiter.reset(gameId)
                        newItems
                    } else items
                    if (refreshedItems.any { it.isDirty })
                        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD)
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
                itemsRateLimiter.reset(gameId)
            }
        }
    }

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = game.switchMap { game ->
        liveData {
            val gameId = game.data?.id ?: BggContract.INVALID_ID
            try {
                if (gameId == BggContract.INVALID_ID) {
                    emit(RefreshableResource.success(emptyList()))
                } else {
                    latestValue?.data?.let { emit(RefreshableResource.refreshing(it)) }
                    val plays = gameRepository.getPlays(gameId)
                    emit(RefreshableResource.refreshing(plays))
                    val lastUpdated = game.data?.updatedPlays ?: System.currentTimeMillis()
                    val refreshedPlays = when {
                        fullPlaysRateLimiter.shouldProcess(gameId, lastUpdated) -> {
                            gameRepository.refreshPlays(gameId)
                            fullPlaysRateLimiter.reset(gameId)
                            gameRepository.getPlays(gameId)
                        }
                        partialPlaysRateLimiter.shouldProcess(gameId, lastUpdated) -> {
                            gameRepository.refreshPartialPlays(gameId)
                            partialPlaysRateLimiter.reset(gameId)
                            gameRepository.getPlays(gameId)
                        }
                        else -> plays
                    }
                    emit(RefreshableResource.success(refreshedPlays))
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error(e, application))
                fullPlaysRateLimiter.reset(gameId)
                partialPlaysRateLimiter.reset(gameId)
            }
        }
    }

    val playColors = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) emptyList() else gameRepository.getPlayColors(gameId))
        }
    }

    fun refresh() {
        _gameId.value?.let { _gameId.value = it }
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { p ->
            game.value?.data?.let { game ->
                viewModelScope.launch {
                    val iconColor = p.getIconSwatch().rgb
                    val darkColor = p.getDarkSwatch().rgb
                    val (winsColor, winnablePlaysColor, allPlaysColor) = p.getPlayCountColors(getApplication())
                    val modified = game.iconColor != iconColor ||
                            game.darkColor != darkColor ||
                            game.winsColor != winsColor ||
                            game.winnablePlaysColor != winnablePlaysColor ||
                            game.allPlaysColor != allPlaysColor
                    if (modified) {
                        gameRepository.updateGameColors(
                            gameId.value ?: BggContract.INVALID_ID,
                            iconColor,
                            darkColor,
                            winsColor,
                            winnablePlaysColor,
                            allPlaysColor,
                        )
                        // refresh() TODO - stop this
                    }
                }
            }
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
            refresh()
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            playRepository.logQuickPlay(gameId, gameName)
        }
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int?) {
        viewModelScope.launch {
            gameCollectionRepository.addCollectionItem(
                gameId.value ?: BggContract.INVALID_ID,
                statuses,
                wishListPriority
            )
        }
    }

    fun createShortcut() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<BggApplication>().applicationContext
            val gameId = _gameId.value ?: BggContract.INVALID_ID
            val gameName = game.value?.data?.name.orEmpty()
            val thumbnailUrl = game.value?.data?.thumbnailUrl.orEmpty()
            val bitmap = imageRepository.fetchThumbnail(thumbnailUrl.ensureHttpsScheme())
            GameActivity.createShortcutInfo(context, gameId, gameName, bitmap)?.let { info ->
                ShortcutManagerCompat.requestPinShortcut(context, info, null)
            }
        }
    }
}
