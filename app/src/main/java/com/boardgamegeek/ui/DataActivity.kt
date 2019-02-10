package com.boardgamegeek.ui

import androidx.fragment.app.Fragment

import com.boardgamegeek.R

class DataActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "Data"

    override fun onCreatePane(): Fragment = DataFragment()

    override fun getDrawerResId(): Int = R.string.title_backup
}
