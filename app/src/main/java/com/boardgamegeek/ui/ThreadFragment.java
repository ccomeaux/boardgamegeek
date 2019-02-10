package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.ThreadSafeResponse;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ThreadFragment extends Fragment implements LoaderManager.LoaderCallbacks<ThreadSafeResponse> {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_THREAD_ID = "THREAD_ID";
	private static final int HELP_VERSION = 2;
	private static final int LOADER_ID = 103;
	private static final int SMOOTH_SCROLL_THRESHOLD = 10;
	private ThreadRecyclerViewAdapter adapter;
	private int threadId;
	private int forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;
	private ShowcaseView showcaseView;
	private int currentAdapterPosition = 0;
	private int latestArticleId = PreferencesUtils.INVALID_ARTICLE_ID;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static ThreadFragment newInstance(int threadId, int forumId, String forumTitle, int gameId, String gameName) {
		Bundle args = new Bundle();
		args.putInt(KEY_THREAD_ID, threadId);
		args.putInt(KEY_FORUM_ID, forumId);
		args.putString(KEY_FORUM_TITLE, forumTitle);
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);

		ThreadFragment fragment = new ThreadFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		readBundle(getArguments());
		View rootView = inflater.inflate(R.layout.fragment_thread, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		threadId = bundle.getInt(KEY_THREAD_ID, BggContract.INVALID_ID);
		forumId = bundle.getInt(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = bundle.getString(KEY_FORUM_TITLE);
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);

		latestArticleId = PreferencesUtils.getThreadArticle(getContext(), threadId);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (latestArticleId != PreferencesUtils.INVALID_ARTICLE_ID) {
			PreferencesUtils.putThreadArticle(getContext(), threadId, latestArticleId);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.thread, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_scroll_last).setVisible(
			latestArticleId != PreferencesUtils.INVALID_ARTICLE_ID &&
				adapter != null &&
				adapter.getItemCount() > 0);
		menu.findItem(R.id.menu_scroll_bottom).setVisible(adapter != null && adapter.getItemCount() > 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_help:
				showHelp();
				return true;
			case R.id.menu_scroll_last:
				scrollToLatestArticle();
				return true;
			case R.id.menu_scroll_bottom:
				scrollToBottom();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void scrollToLatestArticle() {
		if (latestArticleId != PreferencesUtils.INVALID_ARTICLE_ID) {
			scrollToPosition(adapter.getPosition(latestArticleId));
		}
	}

	private void scrollToBottom() {
		scrollToPosition(adapter.getItemCount() - 1);
	}

	private void scrollToPosition(int position) {
		if (position != RecyclerView.NO_POSITION) {
			int difference = Math.abs(currentAdapterPosition - position);
			if (difference <= SMOOTH_SCROLL_THRESHOLD) {
				recyclerView.smoothScrollToPosition(position);
			} else {
				recyclerView.scrollToPosition(position);
			}
		}
	}

	@DebugLog
	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setHasFixedSize(true);
		recyclerView.addOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				currentAdapterPosition = layoutManager.findLastCompletelyVisibleItemPosition();
				if (currentAdapterPosition != RecyclerView.NO_POSITION) {
					long currentArticleId = adapter.getItemId(currentAdapterPosition);
					if (currentArticleId > latestArticleId) {
						latestArticleId = (int) currentArticleId;
					}
				}
			}
		});
	}

	@DebugLog
	private void showHelp() {
		final Builder builder = HelpUtils.getShowcaseBuilder(getActivity());
		if (builder != null) {
			builder.setContentText(R.string.help_thread)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showcaseView.hide();
						HelpUtils.updateHelp(getContext(), HelpUtils.HELP_THREAD_KEY, HELP_VERSION);
					}
				});
			Target viewTarget = getTarget();
			builder.setTarget(viewTarget == null ? Target.NONE : viewTarget);
			showcaseView = builder.build();
			showcaseView.show();
		}
	}

	@DebugLog
	private Target getTarget() {
		final View child = HelpUtils.getRecyclerViewVisibleChild(recyclerView);
		return child == null ? null : new SafeViewTarget(child.findViewById(R.id.view_button));
	}

	@Override
	@DebugLog
	public Loader<ThreadSafeResponse> onCreateLoader(int id, Bundle data) {
		return new ThreadLoader(getActivity(), threadId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<ThreadSafeResponse> loader, ThreadSafeResponse data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ThreadRecyclerViewAdapter(getActivity(), data, forumId, forumTitle, gameId, gameName);
			recyclerView.setAdapter(adapter);
		}

		if (data == null) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else if (data.hasParseError()) {
			emptyView.setText(R.string.parse_error);
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
			maybeShowHelp();
		}
		progressView.hide();

		getActivity().invalidateOptionsMenu();
	}

	@DebugLog
	private void maybeShowHelp() {
		if (HelpUtils.shouldShowHelp(getContext(), HelpUtils.HELP_THREAD_KEY, HELP_VERSION)) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showHelp();
				}
			}, 100);
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<ThreadSafeResponse> loader) {
	}

	private static class ThreadLoader extends BggLoader<ThreadSafeResponse> {
		private final BggService bggService;
		private final int threadId;

		public ThreadLoader(Context context, int threadId) {
			super(context);
			bggService = Adapter.createForXml();
			this.threadId = threadId;
		}

		@Override
		public ThreadSafeResponse loadInBackground() {
			return new ThreadSafeResponse(bggService.thread(threadId));
		}
	}
}
