package com.boardgamegeek.repository

import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeekListRepository {
    suspend fun getGeekLists(sort: String?, page: Int) = withContext(Dispatchers.IO) {
        val response = Adapter.createForJson().geekLists(sort, GeekListsResponse.PAGE_SIZE, page)
        response.mapToEntity()
    }

    suspend fun getGeekList(geekListId: Int) = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().geekList(geekListId, 1)
        response.mapToEntity()
    }
}
