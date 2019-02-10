package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ToolbarUtils;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GamePlaysActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private boolean arePlayersCustomSorted;
	@ColorInt private int iconColor;
	@State int playCount = -1;

	public static void start(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl, String heroImageUrl, boolean arePlayersCustomSorted, @ColorInt int iconColor) {
		Intent starter = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor);
		context.startActivity(starter);
	}

	public static Intent createIntent(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl, String heroImageUrl) {
		return createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, false, 0);
	}

	public static Intent createIntent(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl, String heroImageUrl, boolean arePlayersCustomSorted, @ColorInt int iconColor) {
		Intent intent = new Intent(context, GamePlaysActivity.class);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_HERO_IMAGE_URL, heroImageUrl);
		intent.putExtra(KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		intent.putExtra(KEY_ICON_COLOR, iconColor);
		return intent;
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(gameName);
			}
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL);
		arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
		iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT);

		if (imageUrl == null) imageUrl = "";
		if (thumbnailUrl == null) thumbnailUrl = "";
		if (heroImageUrl == null) heroImageUrl = "";
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstanceForGame(gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		String countDescription = playCount <= 0 ? "" : String.valueOf(playCount);
		ToolbarUtils.setActionBarText(menu, R.id.menu_text, countDescription);
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
