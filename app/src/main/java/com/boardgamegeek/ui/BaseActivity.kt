package com.boardgamegeek.ui

import androidx.annotation.MenuRes
import androidx.core.app.NavUtils
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.boardgamegeek.R
import com.boardgamegeek.service.SyncService

const val INVALID_MENU_ID = 0

/**
 * Registers/unregisters a sticky event bus
 * Shows a toast when user profile is updated
 * Subtitle setter
 * Provides common menu functions:
 * 1. Cancel sync
 * 2. Toggling navigation drawer
 * 3. Inflation helper.
 */
abstract class BaseActivity : AppCompatActivity() {
    protected open val optionsMenuId: Int
        @MenuRes
        get() = INVALID_MENU_ID

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.base, menu)
        if (optionsMenuId != INVALID_MENU_ID) menuInflater.inflate(optionsMenuId, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_cancel_sync)?.isVisible = SyncService.isActiveOrPending(this)
        return true
    }

    protected fun setSubtitle(text: String?) {
        supportActionBar?.subtitle = text ?: ""
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (this is TopLevelActivity) {
                    // bug in ActionBarDrawerToggle
                    return false
                }
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            R.id.menu_cancel_sync -> {
                SyncService.cancelSync(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}