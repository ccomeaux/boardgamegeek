package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

/**
 *  /games/13/colors
 */
class GamesIdColorsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameColors.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_COLORS"

    override val defaultSortOrder = GameColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.GAME_COLORS)
            .whereEquals(GameColors.Columns.GAME_ID, gameId)
    }

    /**
     * @return /games/13/colors/green
     */
    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        values.put(GameColors.Columns.GAME_ID, gameId)
        val rowId = db.insertOrThrow(Tables.GAME_COLORS, null, values)
        return if (rowId != -1L) {
            Games.buildColorsUri(gameId, values.getAsString(GameColors.Columns.COLOR))
        } else null
    }
}
