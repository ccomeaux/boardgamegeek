package com.boardgamegeek.tasks.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.tasks.sync.SyncTask.CompletedEvent;
import com.boardgamegeek.util.NetworkUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.RemoteConfig;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public abstract class SyncTask<T, E extends CompletedEvent> extends AsyncTask<Void, Void, String> {
	@SuppressLint("StaticFieldLeak") @Nullable protected final Context context;
	protected final long startTime;
	protected BggService bggService;
	private Call<T> call;
	private int page = 1;

	SyncTask(@Nullable Context context) {
		this.context = context == null ? null : context.getApplicationContext();
		startTime = System.currentTimeMillis();
	}

	@DebugLog
	@Override
	protected String doInBackground(Void... params) {
		if (context == null) return "Error.";
		bggService = createService();
		if (!isRequestParamsValid())
			return context.getString(R.string.msg_update_invalid_request, context.getString(getTypeDescriptionResId()));
		if (NetworkUtils.isOffline(context)) return context.getString(R.string.msg_offline);
		if (!RemoteConfig.getBoolean(RemoteConfig.KEY_SYNC_ENABLED)) return context.getString(R.string.msg_sync_remotely_disabled);
		try {
			boolean hasMorePages;
			page = 0;
			do {
				page++;
				call = createCall();
				Response<T> response = call.execute();
				if (response.isSuccessful()) {
					if (isResponseBodyValid(response.body())) {
						persistResponse(response.body());
					} else {
						return context.getString(R.string.msg_update_invalid_response,
							context.getString(getTypeDescriptionResId())); // TODO: 3/26/17 include key
					}
				} else {
					Timber.w("Received response %s while syncing %s.", response.code(), context.getString(getTypeDescriptionResId()));
					return PresentationUtils.getHttpErrorMessage(context, response.code());
				}
				if (isCancelled()) break;
				hasMorePages = hasMorePages(response.body());
			} while (hasMorePages);
			if (!isCancelled()) finishSync();
		} catch (Exception e) {
			Timber.w(e, "Exception fetching %1$s: %2$s", context.getString(getTypeDescriptionResId()), e.getMessage());
			return e.getLocalizedMessage();
		}
		return "";
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.w(errorMessage);
		EventBus.getDefault().post(createEvent(errorMessage));
	}

	@Override
	protected void onCancelled() {
		if (call != null) call.cancel();
	}

	protected int getCurrentPage() {
		return page;
	}

	@StringRes
	protected abstract int getTypeDescriptionResId();

	protected abstract Call<T> createCall();

	protected BggService createService() {
		return Adapter.createForXml();
	}

	protected boolean isRequestParamsValid() {
		return context != null;
	}

	protected boolean isResponseBodyValid(T body) {
		return body != null;
	}

	protected abstract void persistResponse(T body);

	protected boolean hasMorePages(T body) {
		return false;
	}

	protected void finishSync() {
	}

	protected abstract E createEvent(String errorMessage);

	protected class CompletedEvent {
		private final String errorMessage;

		public CompletedEvent(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public String getErrorMessage() {
			return errorMessage;
		}
	}
}
