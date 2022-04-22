package com.boardgamegeek.ui.adapter

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GamePagerAdapter(private val activity: FragmentActivity, private val gameId: Int, var gameName: String) : FragmentStateAdapter(activity) {
    var currentPosition = 0
        set(value) {
            field = value
            displayFab()
        }

    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var heroImageUrl = ""
    private var arePlayersCustomSorted = false
    private var isFavorite = false

    @ColorInt
    private var iconColor = Color.TRANSPARENT
    private val tabs = arrayListOf<Tab>()

    private val fab by lazy { activity.findViewById(R.id.fab) as FloatingActionButton }
    private val viewModel by lazy { ViewModelProvider(activity)[GameViewModel::class.java] }
    private val prefs by lazy { activity.preferences() }

    private inner class Tab(
        @field:StringRes val titleResId: Int,
        @field:DrawableRes var imageResId: Int = INVALID_RES_ID,
        val listener: () -> Unit = {}
    )

    init {
        updateTabs()
    }

    fun getPageTitle(position: Int): CharSequence {
        @StringRes val resId = tabs.getOrNull(position)?.titleResId ?: INVALID_RES_ID
        return if (resId != INVALID_RES_ID) activity.getString(resId) else ""
    }

    override fun createFragment(position: Int): Fragment {
        return when (tabs.getOrNull(position)?.titleResId) {
            R.string.title_descr -> GameDescriptionFragment()
            R.string.title_info -> GameFragment()
            R.string.title_credits -> GameCreditsFragment()
            R.string.title_my_games -> GameCollectionFragment()
            R.string.title_plays -> GamePlaysFragment()
            R.string.title_forums -> ForumsFragment.newInstanceForGame(gameId, gameName)
            R.string.links -> GameLinksFragment()
            else -> ErrorFragment()
        }
    }

    override fun getItemCount(): Int = tabs.size

    private fun updateTabs() {
        tabs.clear()
        tabs += Tab(R.string.title_info, R.drawable.ic_baseline_event_available_24) { logPlay() }
        tabs += Tab(R.string.title_credits, R.drawable.ic_baseline_favorite_border_24) { viewModel.updateFavorite(!isFavorite) }
        tabs += Tab(R.string.title_descr, R.drawable.ic_baseline_favorite_border_24) { viewModel.updateFavorite(!isFavorite) }
        if (shouldShowCollection())
            tabs += Tab(R.string.title_my_games, R.drawable.ic_baseline_add_24) { activity.showAndSurvive(CollectionStatusDialogFragment()) }
        if (shouldShowPlays())
            tabs += Tab(R.string.title_plays, R.drawable.ic_baseline_event_available_24) { logPlay() }
        tabs += Tab(R.string.title_forums)
        tabs += Tab(R.string.links)

        viewModel.game.observe(activity) { resource ->
            resource.data?.let { entity ->
                gameName = entity.name
                imageUrl = entity.imageUrl
                thumbnailUrl = entity.thumbnailUrl
                heroImageUrl = entity.heroImageUrl
                arePlayersCustomSorted = entity.customPlayerSort
                iconColor = entity.iconColor
                isFavorite = entity.isFavorite
                updateFavIcon(isFavorite)
                fab.colorize(iconColor)
                fab.setOnClickListener { tabs.getOrNull(currentPosition)?.listener?.invoke() }
                displayFab(false)
            }
        }
    }

    private fun logPlay() {
        when (prefs.logPlayPreference()) {
            LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(activity, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
            LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName)
            LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(activity, gameId, gameName)
        }
    }

    private fun updateFavIcon(isFavorite: Boolean) {
        tabs.find { it.titleResId == R.string.title_credits || it.titleResId == R.string.title_descr }?.let {
            it.imageResId = if (isFavorite) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24
        }
    }

    private fun displayFab(animateChange: Boolean = true) {
        @DrawableRes val resId = tabs.getOrNull(currentPosition)?.imageResId ?: INVALID_RES_ID
        if (resId != INVALID_RES_ID) {
            val existingResId = fab.getTag(R.id.res_id) as? Int? ?: INVALID_RES_ID
            if (resId != existingResId) {
                if (fab.isShown) {
                    if (animateChange) {
                        fab.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                            override fun onHidden(fab: FloatingActionButton?) {
                                super.onHidden(fab)
                                fab?.setImageResource(resId)
                                fab?.show()
                            }
                        })
                    } else {
                        fab.setImageResource(resId)
                        // HACK or else the icon just disappears
                        fab.hide()
                        fab.show()
                    }
                } else {
                    fab.setImageResource(resId)
                    fab.show()
                }
            } else {
                if (!fab.isOrWillBeShown) fab.show()
            }
        } else {
            fab.hide()
        }
        fab.setTag(R.id.res_id, resId)
    }

    // TODO observe these from the view model
    private fun shouldShowPlays() = Authenticator.isSignedIn(activity) && prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true

    private fun shouldShowCollection() = Authenticator.isSignedIn(activity) && prefs.isCollectionSetToSync()

    companion object {
        const val INVALID_RES_ID = 0
    }
}
