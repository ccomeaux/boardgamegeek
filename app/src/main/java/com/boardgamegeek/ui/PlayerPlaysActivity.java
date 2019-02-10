package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.util.ToolbarUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import hugo.weaving.DebugLog;

public class PlayerPlaysActivity extends SimpleSinglePaneActivity {
	private static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	private String name;
	private int playCount = -1;

	public static void start(Context context, String playerName) {
		Intent starter = new Intent(context, PlayerPlaysActivity.class);
		starter.putExtra(KEY_PLAYER_NAME, playerName);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setSubtitle(name);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("PlayerPlays")
				.putContentName(name));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		name = intent.getStringExtra(KEY_PLAYER_NAME);
	}

	@NonNull
	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstanceForPlayer(name);
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.player;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playCount < 0 ? "" : String.valueOf(playCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(@NonNull PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}