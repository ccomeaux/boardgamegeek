package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables

class CategoriesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_ITEM_TYPE

    override val path = "$PATH_CATEGORIES/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
            .table(Tables.CATEGORIES)
            .whereEquals(Categories.Columns.CATEGORY_ID, categoryId)
    }
}
