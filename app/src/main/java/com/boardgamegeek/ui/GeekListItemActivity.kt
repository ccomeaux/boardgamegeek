package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class GeekListItemActivity : HeroTabActivity() {
    private var geekListId = 0
    private var geekListTitle = ""
    private var order = 0
    private var geekListItemEntity = GeekListItemEntity()

    private val adapter: GeekListItemPagerAdapter by lazy {
        GeekListItemPagerAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()
        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        order = intent.getIntExtra(KEY_ORDER, 0)
        geekListItemEntity = intent.getParcelableExtra(KEY_ITEM) ?: GeekListItemEntity()

        initializeViewPager()

        safelySetTitle(geekListItemEntity.objectName)
        if (savedInstanceState == null && geekListItemEntity.objectId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekListItem")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListItemEntity.objectId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListItemEntity.objectName)
            }
        }
        loadToolbarImage(geekListItemEntity.imageId)
    }

    override val optionsMenuId = R.menu.view

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (geekListId != BggContract.INVALID_ID) {
                    GeekListActivity.startUp(this, geekListId, geekListTitle)
                    finish()
                }
                else onBackPressed()
                true
            }
            R.id.menu_view -> {
                if (geekListItemEntity.isBoardGame) {
                    if (geekListItemEntity.objectId == BggContract.INVALID_ID || geekListItemEntity.objectName.isBlank()) false else {
                        start(this, geekListItemEntity.objectId, geekListItemEntity.objectName)
                        true
                    }
                }
                else {
                    if (geekListItemEntity.objectUrl.isBlank()) false else {
                        link(geekListItemEntity.objectUrl)
                        true
                    }
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun createAdapter() = adapter

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> getString(R.string.title_description)
            1 -> getString(R.string.title_comments)
            else -> ""
        }
    }

    inner class GeekListItemPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GeekListItemFragment.newInstance(order, geekListTitle, geekListItemEntity)
                1 -> GeekListItemCommentsFragment.newInstance(geekListItemEntity.comments)
                else -> ErrorFragment()
            }
        }

        override fun getItemCount() = 2
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "GEEK_LIST_ITEM"

        fun start(context: Context, geekList: GeekListEntity, item: GeekListItemEntity, order: Int) {
            context.startActivity<GeekListItemActivity>(
                KEY_ID to geekList.id,
                KEY_TITLE to geekList.title,
                KEY_ORDER to order,
                KEY_ITEM to item,
            )
        }
    }
}
