package com.boardgamegeek.tasks.sync

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import retrofit2.Call
import timber.log.Timber

class SyncPlaysByDateTask(
        private val application: BggApplication,
        private val timeInMillis: Long,
        errorMessageLiveData: MutableLiveData<String>,
        syncingLiveData: MutableLiveData<Boolean>) :
        SyncTask<PlaysResponse>(application.applicationContext, errorMessageLiveData, syncingLiveData) {
    private val username = AccountUtils.getUsername(context)
    private val dao = PlayDao(application)
    private val mapper = PlayMapper()

    @get:StringRes
    override val typeDescriptionResId: Int
        get() = R.string.title_plays

    override fun createCall(): Call<PlaysResponse>? {
        val date = timeInMillis.asDateForApi()
        return bggService?.playsByDate(username, date, date, currentPage)
    }

    override val isRequestParamsValid: Boolean
        get() = timeInMillis > 0L && !username.isNullOrBlank()

    override fun persistResponse(body: PlaysResponse?) {
        body?.plays?.let {
            val plays = mapper.map(it, startTime)
            dao.save(plays, startTime)
        }
        Timber.i("Synced plays for %s (page %,d)", timeInMillis.asDateForApi(), currentPage)
    }

    override fun hasMorePages(body: PlaysResponse?): Boolean {
        return body?.hasMorePages() == true
    }

    override fun finishSync() {
        CalculatePlayStatsTask(application).executeAsyncTask()
    }
}