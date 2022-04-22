package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    enum class CollectionSort {
        NAME, RATING
    }

    private val repository = CategoryRepository(getApplication())

    private val _category = MutableLiveData<Pair<Int, CollectionSort>>()

    fun setId(id: Int) {
        if (_category.value?.first != id)
            _category.value = id to (_category.value?.second ?: CollectionSort.RATING)
    }

    fun setSort(sortType: CollectionSort) {
        if (_category.value?.second != sortType)
            _category.value = (_category.value?.first ?: BggContract.INVALID_ID) to sortType
    }

    fun refresh() {
        _category.value?.let { _category.value = it }
    }

    val sort = _category.map {
        it.second
    }

    val collection = _category.switchMap { c ->
        liveData {
            val collection = when (c.first) {
                BggContract.INVALID_ID -> emptyList()
                else -> when (c.second) {
                    CollectionSort.NAME -> repository.loadCollection(c.first, CollectionDao.SortType.NAME)
                    CollectionSort.RATING -> repository.loadCollection(c.first, CollectionDao.SortType.RATING)
                }
            }
            emit(collection)
        }
    }
}
