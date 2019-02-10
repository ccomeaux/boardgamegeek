package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.util.ToolbarUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import hugo.weaving.DebugLog;

public class PlaysActivity extends SimpleSinglePaneActivity {
	private int playCount;
	private String sortName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Plays"));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstance();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		boolean hide = playCount <= 0;
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			hide ? "" : String.format("%,d", playCount),
			hide ? "" : sortName);
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(PlaysFilterChangedEvent event) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (event.getType() == PlaysFragment.FILTER_TYPE_STATUS_ALL) {
				actionBar.setSubtitle("");
			} else {
				actionBar.setSubtitle(event.getDescription());
			}
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysSortChangedEvent event) {
		sortName = event.getDescription();
		supportInvalidateOptionsMenu();
	}
}
