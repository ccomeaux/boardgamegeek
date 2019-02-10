package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.NotificationUtils;

import androidx.annotation.Nullable;

public class SyncService extends Service {
	public static final String EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE";
	public static final int FLAG_SYNC_NONE = 0;
	public static final int FLAG_SYNC_COLLECTION_DOWNLOAD = 1;
	public static final int FLAG_SYNC_COLLECTION_UPLOAD = 1 << 1;
	public static final int FLAG_SYNC_BUDDIES = 1 << 2;
	public static final int FLAG_SYNC_PLAYS_DOWNLOAD = 1 << 3;
	public static final int FLAG_SYNC_PLAYS_UPLOAD = 1 << 4;
	public static final int FLAG_SYNC_GAMES = 1 << 5;
	public static final int FLAG_SYNC_COLLECTION = FLAG_SYNC_COLLECTION_DOWNLOAD | FLAG_SYNC_COLLECTION_UPLOAD | FLAG_SYNC_GAMES;
	public static final int FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD | FLAG_SYNC_PLAYS_UPLOAD;
	public static final int FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION | FLAG_SYNC_BUDDIES | FLAG_SYNC_PLAYS;
	public static final String ACTION_CANCEL_SYNC = "com.boardgamegeek.ACTION_SYNC_CANCEL";

	private static final Object SYNC_ADAPTER_LOCK = new Object();
	@Nullable private static SyncAdapter syncAdapter = null;

	@Override
	public void onCreate() {
		synchronized (SYNC_ADAPTER_LOCK) {
			if (syncAdapter == null) {
				syncAdapter = new SyncAdapter((BggApplication) getApplication());
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter != null ? syncAdapter.getSyncAdapterBinder() : null;
	}

	public static void sync(Context context, int syncType) {
		Account account = Authenticator.getAccount(context);
		if (account != null) {
			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			extras.putInt(EXTRA_SYNC_TYPE, syncType);
			ContentResolver.requestSync(account, BggContract.CONTENT_AUTHORITY, extras);
		}
	}

	public static void cancelSync(Context context) {
		NotificationUtils.cancel(context, NotificationUtils.TAG_SYNC_PROGRESS);
		Account account = Authenticator.getAccount(context);
		if (account != null) {
			ContentResolver.cancelSync(account, BggContract.CONTENT_AUTHORITY);
		}
	}

	public static boolean isActiveOrPending(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) return false;
		boolean syncActive = ContentResolver.isSyncActive(account, BggContract.CONTENT_AUTHORITY);
		boolean syncPending = ContentResolver.isSyncPending(account, BggContract.CONTENT_AUTHORITY);
		return syncActive || syncPending;
	}
}
