package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.CollectionViewAdapter
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import timber.log.Timber

class CollectionActivity : TopLevelSinglePaneActivity() {
    private var viewId: Long = 0
    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = BggContract.INVALID_ID.toLong()
    private var hideNavigation = false
    private var snackbar: Snackbar? = null

    private val viewModel by viewModels<CollectionViewViewModel>()
    private val adapter: CollectionViewAdapter by lazy { CollectionViewAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            if (hideNavigation) {
                it.setHomeButtonEnabled(false)
                it.setDisplayHomeAsUpEnabled(false)
                it.setTitle(R.string.app_name)
            } else {
                it.setDisplayShowTitleEnabled(false)
                it.setDisplayShowCustomEnabled(true)
                it.setCustomView(R.layout.actionbar_collection)
            }
        }

        viewModel.errorMessage.observe(this) {
            it.getContentIfNotHandled()?.let { message ->
                if (message.isBlank()) {
                    snackbar?.dismiss()
                } else {
                    snackbar = rootContainer?.longSnackbar(message)
                }
            }
        }
        viewModel.selectedViewId.observe(this) { id: Long -> viewId = id }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
            }
            Timber.i("Select view ?? from UI init")
            selectView(
                if (hideNavigation) CollectionView.DEFAULT_DEFAULT_ID else intent.getLongExtra(KEY_VIEW_ID, viewModel.defaultViewId),
                !hideNavigation
            )
        }
    }

    override fun readIntent(intent: Intent) {
        isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT == getIntent().action
        changingGamePlayId = getIntent().getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())
        hideNavigation = isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID.toLong()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        findViewById<AppCompatSpinner>(R.id.menu_spinner)?.let {
            it.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Timber.i("Select view $id from UI menu")
                    selectView(id)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { // Do nothing
                    Timber.i("Nothing selected from UI menu")
                }
            }
            it.adapter = adapter
            viewModel.views.observe(this) { collectionViews: List<CollectionViewEntity?> ->
                if (collectionViews.isNotEmpty()) {
                    adapter.clear()
                    adapter.addAll(collectionViews)
                }
                it.setSelection(adapter.findIndexOf(viewId))
            }
        }
        return true
    }

    private fun selectView(id: Long, logEvent: Boolean = true) {
        Timber.i("Select view $id from UI")
        viewModel.selectView(id)
        if (logEvent) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
            }
        }
    }

    override val navigationItemId: Int = R.id.collection

    override val optionsMenuId: Int = R.menu.search

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (hideNavigation) {
            menu.findItem(R.id.menu_search)?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_search) {
            startActivity(intentFor<SearchResultsActivity>())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreatePane(): Fragment {
        return if (changingGamePlayId != BggContract.INVALID_ID.toLong()) {
            CollectionFragment.newInstanceForPlayGameChange(changingGamePlayId)
        } else {
            CollectionFragment.newInstance(isCreatingShortcut)
        }
    }

    companion object {
        private const val KEY_VIEW_ID = "VIEW_ID"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun startForGameChange(context: Context, playId: Long) {
            context.startActivity<CollectionActivity>(KEY_CHANGING_GAME_PLAY_ID to playId)
        }

        fun createShortcutInfo(context: Context, viewId: Long, viewName: String): ShortcutInfoCompat {
            val intent = context.intentFor<CollectionActivity>(KEY_VIEW_ID to viewId)
                .clearTask()
                .newTask()
                .apply { action = Intent.ACTION_VIEW }
            return ShortcutInfoCompat.Builder(context, createShortcutName(viewId))
                .setShortLabel(viewName.toShortLabel())
                .setLongLabel(viewName.toLongLabel())
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
                .setIntent(intent)
                .build()
        }

        fun createShortcutName(viewId: Long) = "collection_view-$viewId"
    }
}
