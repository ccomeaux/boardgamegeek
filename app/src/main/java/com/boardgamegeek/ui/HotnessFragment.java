package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.model.HotnessResponse;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class HotnessFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<HotnessResponse>>, ActionMode.Callback {
	private static final int LOADER_ID = 1;

	private HotGamesAdapter adapter;
	private ActionMode actionMode;
	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_hotness, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setHasFixedSize(true);
	}

	@Override
	public Loader<SafeResponse<HotnessResponse>> onCreateLoader(int id, Bundle data) {
		return new HotnessLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<SafeResponse<HotnessResponse>> loader, SafeResponse<HotnessResponse> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new HotGamesAdapter(getActivity(),
				data == null || data.getBody() == null ? new ArrayList<HotGame>() : data.getBody().games,
				new Callback() {
					@Override
					public boolean onItemClick(int position) {
						if (actionMode == null) return false;
						toggleSelection(position);
						return true;
					}

					@Override
					public boolean onItemLongClick(int position) {
						if (actionMode != null) return false;
						actionMode = getActivity().startActionMode(HotnessFragment.this);
						if (actionMode == null) return false;
						toggleSelection(position);
						return true;
					}

					private void toggleSelection(int position) {
						adapter.toggleSelection(position);
						int count = adapter.getSelectedItemCount();
						if (count == 0) {
							actionMode.finish();
						} else {
							actionMode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));
							actionMode.invalidate();
						}
					}
				});
			recyclerView.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}

		if (data == null) {
			AnimationUtils.fadeOut(recyclerView);
			AnimationUtils.fadeIn(emptyView);
		} else if (data.hasError()) {
			emptyView.setText(getString(R.string.empty_http_error, data.getErrorMessage()));
			AnimationUtils.fadeOut(recyclerView);
			AnimationUtils.fadeIn(emptyView);
		} else {
			AnimationUtils.fadeOut(emptyView);
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		progressView.hide();
	}

	@Override
	public void onLoaderReset(Loader<SafeResponse<HotnessResponse>> loader) {
	}

	private static class HotnessLoader extends BggLoader<SafeResponse<HotnessResponse>> {
		private final BggService bggService;

		public HotnessLoader(Context context) {
			super(context);
			bggService = Adapter.createForXml();
		}

		@Override
		public SafeResponse<HotnessResponse> loadInBackground() {
			Call<HotnessResponse> call = bggService.getHotness(BggService.HOTNESS_TYPE_BOARDGAME);
			return new SafeResponse<>(call);
		}
	}

	public interface Callback {
		boolean onItemClick(int position);

		boolean onItemLongClick(int position);
	}

	public class HotGamesAdapter extends RecyclerView.Adapter<HotGamesAdapter.ViewHolder> {
		private final LayoutInflater inflater;
		private final List<HotGame> games;
		private final Callback callback;
		private final SparseBooleanArray selectedItems;

		public HotGamesAdapter(Context context, List<HotGame> games, Callback callback) {
			this.games = games;
			this.callback = callback;
			inflater = LayoutInflater.from(context);
			selectedItems = new SparseBooleanArray();
			setHasStableIds(true);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.row_hotness, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			holder.bind(getItem(position), position);
		}

		@Override
		public int getItemCount() {
			return games == null ? 0 : games.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public HotGame getItem(int position) {
			return games.get(position);
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			private int gameId;
			private String gameName;
			private String thumbnailUrl;
			@BindView(R.id.name) TextView name;
			@BindView(R.id.year) TextView year;
			@BindView(R.id.rank) TextView rank;
			@BindView(R.id.thumbnail) ImageView thumbnail;

			public ViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(HotGame game, final int position) {
				if (game == null) return;
				gameId = game.getId();
				gameName = game.getName();
				thumbnailUrl = game.getThumbnailUrl();
				name.setText(game.getName());
				year.setText(PresentationUtils.describeYear(name.getContext(), game.getYearPublished()));
				rank.setText(String.valueOf(game.getRank()));
				ImageUtils.loadThumbnail(thumbnail, game.getThumbnailUrl());

				itemView.setActivated(selectedItems.get(position, false));

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean handled = false;
						if (callback != null) {
							handled = callback.onItemClick(position);
						}
						if (!handled) {
							GameActivity.start(getContext(), gameId, gameName, thumbnailUrl);
						}
					}
				});

				itemView.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return callback != null && callback.onItemLongClick(position);
					}
				});
			}
		}

		public void toggleSelection(int position) {
			if (selectedItems.get(position, false)) {
				selectedItems.delete(position);
			} else {
				selectedItems.put(position, true);
			}
			notifyItemChanged(position);
		}

		public void clearSelections() {
			selectedItems.clear();
			notifyDataSetChanged();
		}

		public int getSelectedItemCount() {
			return selectedItems.size();
		}

		public List<Integer> getSelectedItems() {
			List<Integer> items = new ArrayList<>(selectedItems.size());
			for (int i = 0; i < selectedItems.size(); i++) {
				items.add(selectedItems.keyAt(i));
			}
			return items;
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		adapter.clearSelections();
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		int count = adapter.getSelectedItemCount();
		menu.findItem(R.id.menu_log_play).setVisible(Authenticator.isSignedIn(getContext()) && count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		menu.findItem(R.id.menu_log_play_quick).setVisible(Authenticator.isSignedIn(getContext()) && PreferencesUtils.showQuickLogPlay(getActivity()));
		menu.findItem(R.id.menu_link).setVisible(count == 1);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		actionMode = null;
		adapter.clearSelections();
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		final List<Integer> selectedItems = adapter.getSelectedItems();
		if (selectedItems.size() == 0) return false;
		HotGame game = adapter.getItem(selectedItems.get(0));
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				LogPlayActivity.logPlay(getContext(), game.getId(), game.getName(), game.getThumbnailUrl(), game.getThumbnailUrl(), "", false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, adapter.getSelectedItemCount());
				Snackbar.make(containerView, text, Snackbar.LENGTH_SHORT).show();
				for (int position : selectedItems) {
					HotGame g = adapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.getId(), g.getName());
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				final String shareMethod = "Hotness";
				if (adapter.getSelectedItemCount() == 1) {
					ActivityUtils.shareGame(getActivity(), game.getId(), game.getName(), shareMethod);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(adapter.getSelectedItemCount());
					for (int position : selectedItems) {
						HotGame g = adapter.getItem(position);
						games.add(Pair.create(g.getId(), g.getName()));
					}
					ActivityUtils.shareGames(getActivity(), games, shareMethod);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.getId());
				return true;
		}
		return false;
	}
}
