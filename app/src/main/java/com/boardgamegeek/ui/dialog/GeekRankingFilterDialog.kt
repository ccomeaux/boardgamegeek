package com.boardgamegeek.ui.dialog

import android.content.Context

import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.GeekRankingFilterer
import kotlin.math.roundToInt

class GeekRankingFilterDialog : SliderFilterDialog() {
    override fun getType(context: Context) = GeekRankingFilterer(context).type
    override val titleResId = R.string.menu_geek_ranking
    override val valueFrom = GeekRankingFilterer.lowerBound.toFloat()
    override val valueTo = GeekRankingFilterer.upperBound.toFloat()
    override val supportsSlider = false

    override fun initValues(filter: CollectionFilterer?): InitialValues {
        val f = filter as? GeekRankingFilterer
        return InitialValues(
            (f?.min ?: GeekRankingFilterer.lowerBound).toFloat(),
            (f?.max ?: GeekRankingFilterer.upperBound).toFloat(),
            f?.includeUnranked ?: false,
        )
    }

    override fun createFilterer(context: Context): CollectionFilterer {
        return GeekRankingFilterer(context).apply {
            min = low.roundToInt()
            max = high.roundToInt()
            includeUnranked = checkboxIsChecked
        }
    }

    override fun describeRange(context: Context): String {
        return (createFilterer(context) as? GeekRankingFilterer)?.describeRange().orEmpty()
    }
}
