package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.events.CollectionStatusChangedEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class BuddyCollectionActivity extends SimpleSinglePaneActivity {
	private static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	private String buddyName;

	public static void start(Context context, String buddyName) {
		Intent starter = new Intent(context, BuddyCollectionActivity.class);
		starter.putExtra(KEY_BUDDY_NAME, buddyName);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!TextUtils.isEmpty(buddyName)) {
			ActionBar bar = getSupportActionBar();
			if (bar != null) {
				bar.setSubtitle(buddyName);
			}
		}
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("BuddyCollection")
				.putContentId(buddyName));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		buddyName = intent.getStringExtra(KEY_BUDDY_NAME);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return BuddyCollectionFragment.newInstance(buddyName);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				BuddyActivity.startUp(this, buddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionStatusChangedEvent event) {
		String text = buddyName;
		if (!TextUtils.isEmpty(event.getDescription())) {
			text += " - " + event.getDescription();
		}
		ActionBar bar = getSupportActionBar();
		if (bar != null) {
			bar.setSubtitle(text);
		}
	}
}
