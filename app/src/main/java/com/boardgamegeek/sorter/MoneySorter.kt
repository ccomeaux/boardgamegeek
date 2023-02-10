package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asMoney
import java.text.DecimalFormat
import kotlin.math.ceil

abstract class MoneySorter(context: Context) : CollectionSorter(context) {
    protected abstract fun amount(item: CollectionItemEntity): Double
    protected abstract fun currency(item: CollectionItemEntity): String

    override fun sortAscending(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> {
        return items.sortedWith(compareBy<CollectionItemEntity> { currency(it) }.thenBy { amount(it) })
    }

    override fun sortDescending(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> {
        return items.sortedWith(compareByDescending<CollectionItemEntity> { currency(it) }.thenByDescending { amount(it) })
    }

    private fun round(value: Double) = (ceil(value / 10).toInt() * 10).toDouble()

    override fun getHeaderText(item: CollectionItemEntity) = round(amount(item)).asMoney(currency(item), DecimalFormat("0")).ifEmpty { MISSING_DATA }

    override fun getDisplayInfo(item: CollectionItemEntity) = amount(item).asMoney(currency(item)).ifEmpty { MISSING_DATA }

    companion object {
        private const val MISSING_DATA = "-"
    }
}
