package com.boardgamegeek.service

import android.app.PendingIntent
import android.content.Intent
import android.content.SyncResult
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat.Action
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.mappers.mapToFormBodyForDelete
import com.boardgamegeek.mappers.mapToFormBodyForUpsert
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.ui.GamePlaysActivity
import com.boardgamegeek.ui.LogPlayActivity
import com.boardgamegeek.ui.PlayActivity
import com.boardgamegeek.ui.PlaysActivity
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import kotlin.time.Duration.Companion.seconds

class SyncPlaysUpload(
    application: BggApplication,
    syncResult: SyncResult,
    private val repository: PlayRepository,
    private val httpClient: OkHttpClient,
) : SyncUploadTask(application, syncResult) {
    private var currentPlay = PlayForNotification()
    private val gameIds = mutableSetOf<Int>()

    inner class PlayForNotification(
        var internalId: Long = INVALID_ID.toLong(),
        val gameId: Int = INVALID_ID,
        val gameName: String = ""
    ) {
        var heroImageUrl: String = ""
        var customPlayerSort: Boolean = false

        val internalIdAsInt = if (internalId < Int.MAX_VALUE) internalId.toInt() else (internalId % Int.MAX_VALUE).toInt()
    }

    override val syncType = SyncService.FLAG_SYNC_PLAYS_UPLOAD

    override val notificationTitleResId = R.string.sync_notification_title_play_upload

    override val summarySuffixResId = R.plurals.plays_suffix

    override val notificationSummaryIntent = context.intentFor<PlaysActivity>()

    override val notificationIntent: Intent
        get() = if (currentPlay.internalId == INVALID_ID.toLong())
            GamePlaysActivity.createIntent(
                context,
                currentPlay.gameId,
                currentPlay.gameName,
                currentPlay.heroImageUrl
            )
        else
            PlayActivity.createIntent(context, currentPlay.internalId)

    override val notificationMessageTag = NotificationTags.UPLOAD_PLAY

    override val notificationErrorTag = NotificationTags.UPLOAD_PLAY_ERROR

    override val notificationSummaryMessageId = R.string.sync_notification_plays_upload

    override fun execute() {
        deletePendingPlays()
        updatePendingPlays()
        runBlocking {
            gameIds.forEach { repository.updateGamePlayCount(it) }
            repository.calculatePlayStats()
        }
    }

    private fun updatePendingPlays() {
        val pendingPlays = runBlocking { repository.getUpdatingPlays() }
        var currentNumberOfPlays = 0
        val totalNumberOfPlays = pendingPlays.size
        updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_update, totalNumberOfPlays, totalNumberOfPlays)
        pendingPlays.forEach { play ->
            if (isCancelled) return
            if (wasSleepInterrupted(1.seconds, false)) return

            updateProgressNotificationAsPlural(
                R.plurals.sync_notification_plays_update_increment,
                totalNumberOfPlays,
                ++currentNumberOfPlays,
                totalNumberOfPlays,
            )

            try {
                val response = postPlayUpdate(play)
                when {
                    response.hasAuthError() -> {
                        syncResult.stats.numAuthExceptions++
                        Authenticator.clearPassword(context)
                        return
                    }
                    response.hasInvalidIdError() -> {
                        syncResult.stats.numConflictDetectedExceptions++
                        notifyUploadError(context.getText(R.string.msg_play_update_bad_id, play.playId))
                    }
                    response.hasError() -> {
                        syncResult.stats.numIoExceptions++
                        notifyUploadError(response.errorMessage)
                    }
                    response.playCount < 0 -> {
                        syncResult.stats.numIoExceptions++
                        notifyUploadError(context.getString(R.string.msg_play_update_null_response))
                    }
                    else -> {
                        syncResult.stats.numUpdates++
                        val message = when {
                            play.isSynced -> context.getText(R.string.msg_play_updated)
                            play.quantity > 0 -> context.getText(
                                R.string.msg_play_added_quantity,
                                getPlayCountDescription(response.playCount, play.quantity)
                            )
                            else -> context.getText(R.string.msg_play_added)
                        }

                        currentPlay = PlayForNotification(play.internalId, play.gameId, play.gameName)
                        notifyUser(play, message)

                        runBlocking { repository.markAsSynced(play.internalId, response.playId) }
                        gameIds += play.gameId
                    }
                }
            } catch (e: Exception) {
                syncResult.stats.numParseExceptions++
                notifyUploadError(e.localizedMessage.orEmpty())
            }
        }
    }

    private fun deletePendingPlays() {
        val deletedPlays = runBlocking { repository.getDeletingPlays() }
        var currentNumberOfPlays = 0
        val totalNumberOfPlays = deletedPlays.size
        updateProgressNotificationAsPlural(R.plurals.sync_notification_plays_delete, totalNumberOfPlays, totalNumberOfPlays)

        deletedPlays.forEach { play ->
            if (isCancelled) return
            if (wasSleepInterrupted(1.seconds, false)) return

            updateProgressNotificationAsPlural(
                R.plurals.sync_notification_plays_delete_increment,
                totalNumberOfPlays,
                ++currentNumberOfPlays,
                totalNumberOfPlays
            )

            try {
                currentPlay = PlayForNotification(play.internalId, play.gameId, play.gameName)
                if (play.isSynced) {
                    val response = postPlayDelete(play.playId)
                    when {
                        response.isSuccessful -> {
                            syncResult.stats.numDeletes++
                            runBlocking { repository.delete(play.internalId) }
                            gameIds += play.gameId
                            notifyUserOfDelete(R.string.msg_play_deleted, play)
                        }
                        response.hasInvalidIdError() -> {
                            syncResult.stats.numConflictDetectedExceptions++
                            runBlocking { repository.delete(play.internalId) }
                            notifyUserOfDelete(R.string.msg_play_deleted, play)
                        }
                        response.hasAuthError() -> {
                            syncResult.stats.numAuthExceptions++
                            Authenticator.clearPassword(context)
                            return
                        }
                        else -> {
                            syncResult.stats.numIoExceptions++
                            notifyUploadError(response.errorMessage)
                        }
                    }
                } else {
                    syncResult.stats.numDeletes++
                    runBlocking { repository.delete(play.internalId) }
                    gameIds += play.gameId
                    notifyUserOfDelete(R.string.msg_play_deleted_draft, play)
                }
            } catch (e: Exception) {
                syncResult.stats.numParseExceptions++
                notifyUploadError(e.localizedMessage.orEmpty())
            }
        }
    }

    private fun postPlayUpdate(play: PlayEntity): PlaySaveResponse {
        val request = Builder()
            .url(GEEK_PLAY_URL)
            .post(play.mapToFormBodyForUpsert().build())
            .build()
        return PlaySaveResponse(httpClient, request)
    }

    private fun postPlayDelete(playId: Int): PlayDeleteResponse {
        val request = Builder()
            .url(GEEK_PLAY_URL)
            .post(playId.mapToFormBodyForDelete().build())
            .build()
        return PlayDeleteResponse(httpClient, request)
    }

    private fun getPlayCountDescription(count: Int, quantity: Int): String {
        return when (quantity) {
            1 -> count.toOrdinal()
            2 -> "${(count - 1).toOrdinal()} & ${count.toOrdinal()}"
            else -> "${(count - quantity + 1).toOrdinal()} - ${count.toOrdinal()}"
        }
    }

    private fun notifyUserOfDelete(@StringRes messageId: Int, play: PlayEntity) {
        context.cancelNotification(notificationMessageTag, currentPlay.internalIdAsInt.toLong())
        currentPlay.internalId = INVALID_ID.toLong()
        notifyUser(play, context.getText(messageId, play.gameName))
    }

    private fun notifyUser(play: PlayEntity, message: CharSequence) {
        currentPlay.heroImageUrl = play.heroImageUrl
        currentPlay.customPlayerSort = play.gameIsCustomSorted
        notifyUser(
            play.gameName,
            message,
            currentPlay.internalIdAsInt,
            play.heroImageUrl,
        )
    }

    override fun createMessageAction(): Action? {
        if (currentPlay.internalId != INVALID_ID.toLong()) {
            val intent = LogPlayActivity.createRematchIntent(
                context,
                currentPlay.internalId,
                currentPlay.gameId,
                currentPlay.gameName,
                currentPlay.heroImageUrl,
                currentPlay.customPlayerSort,
            )
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            val builder = Action.Builder(
                R.drawable.ic_baseline_replay_24,
                context.getString(R.string.rematch),
                pendingIntent
            )
            return builder.build()
        }
        return null
    }

    companion object {
        const val GEEK_PLAY_URL = "https://boardgamegeek.com/geekplay.php"
    }
}
