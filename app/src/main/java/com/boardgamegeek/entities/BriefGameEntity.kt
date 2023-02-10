package com.boardgamegeek.entities

data class BriefGameEntity(
    val internalId: Long,
    val gameId: Int,
    val gameName: String,
    val collectionName: String,
    val yearPublished: Int,
    val collectionYearPublished: Int,
    val collectionThumbnailUrl: String,
    val gameThumbnailUrl: String,
    val gameHeroImageUrl: String,
    val personalRating: Double = 0.0,
    val isFavorite: Boolean = false,
    val subtype: GameEntity.Subtype? = GameEntity.Subtype.BOARDGAME,
    val playCount: Int = 0,
) {
    val name = collectionName.ifBlank { gameName }
    val thumbnailUrl = collectionThumbnailUrl.ifBlank { gameThumbnailUrl }
    val year = if (collectionYearPublished == YEAR_UNKNOWN) yearPublished else collectionYearPublished

    companion object {
        const val YEAR_UNKNOWN = GameEntity.YEAR_UNKNOWN
    }
}
