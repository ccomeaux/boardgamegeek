package com.boardgamegeek.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.boardgamegeek.extensions.use
import com.boardgamegeek.provider.BggContract.Collection
import timber.log.Timber

class UpdateCollectionItemTextTask(context: Context?, gameId: Int, collectionId: Int, internalId: Long, private val text: String, private val textColumn: String, private val timestampColumn: String) : UpdateCollectionItemTask(context, gameId, collectionId, internalId) {
    override fun updateResolver(resolver: ContentResolver, internalId: Long): Boolean {
        val item = Item.fromResolver(resolver, internalId, textColumn) ?: return false
        if (item.text != text) {
            val values = ContentValues(2)
            values.put(textColumn, text)
            values.put(timestampColumn, System.currentTimeMillis())
            resolver.update(Collection.buildUri(internalId), values, null, null)
            return true
        }
        return false
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result == true) {
            Timber.i("Updated game ID $gameId, collection ID $collectionId with text \"$text\"")
        } else {
            Timber.i("No text to update for game ID $gameId, collection ID $collectionId.")
        }
    }

    data class Item(val text: String) {
        companion object {
            fun fromResolver(contentResolver: ContentResolver, internalId: Long, columnName: String): Item? {
                val cursor = contentResolver.query(Collection.buildUri(internalId),
                        arrayOf(columnName),
                        null,
                        null,
                        null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        return Item(it.getString(0) ?: "")
                    }
                }
                return null
            }
        }
    }
}
