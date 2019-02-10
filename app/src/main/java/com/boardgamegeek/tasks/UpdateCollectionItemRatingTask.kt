package com.boardgamegeek.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.boardgamegeek.extensions.use
import com.boardgamegeek.provider.BggContract.Collection
import timber.log.Timber

class UpdateCollectionItemRatingTask(context: Context?, gameId: Int, collectionId: Int, internalId: Long, private val rating: Double) : UpdateCollectionItemTask(context, gameId, collectionId, internalId) {
    override fun updateResolver(resolver: ContentResolver, internalId: Long): Boolean {
        val item = Item.fromResolver(resolver, internalId) ?: return false
        if (rating != item.rating) {
            val values = ContentValues(2)
            values.put(Collection.RATING, rating)
            values.put(Collection.RATING_DIRTY_TIMESTAMP, System.currentTimeMillis())
            resolver.update(Collection.buildUri(internalId), values, null, null)
            return true
        }
        return false
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result == true) {
            Timber.i("Updated game ID $gameId, collection ID $collectionId with rating $rating.")
        } else {
            Timber.i("No rating to update for game ID $gameId, collection ID $collectionId.")
        }
    }

    data class Item(val rating: Double) {
        companion object {
            fun fromResolver(contentResolver: ContentResolver, internalId: Long): Item? {
                val cursor = contentResolver.query(Collection.buildUri(internalId),
                        arrayOf(Collection.RATING),
                        null,
                        null,
                        null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        return Item(it.getDouble(0))
                    }
                }
                return null
            }
        }
    }
}
