package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.model.GeekListResponse;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

public class GeekListActivity extends TabActivity implements LoaderManager.LoaderCallbacks<SafeResponse<GeekListResponse>> {
	private static final String KEY_ID = "GEEK_LIST_ID";
	private static final String KEY_TITLE = "GEEK_LIST_TITLE";
	private static final int LOADER_ID = 1;
	private int geekListId;
	private String geekListTitle;
	private GeekList geekList;
	private List<GeekListItem> geekListItems;
	private String errorMessage;
	private String descriptionFragmentTag;
	private String itemsFragmentTag;
	private GeekListPagerAdapter adapter;

	public static void start(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int id, String title) {
		Intent starter = new Intent(context, GeekListActivity.class);
		starter.putExtra(KEY_ID, id);
		starter.putExtra(KEY_TITLE, title);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(KEY_TITLE);
		safelySetTitle(geekListTitle);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekList")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				//noinspection SpellCheckingInspection
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@NotNull
	@Override
	protected FragmentPagerAdapter createAdapter() {
		adapter = new GeekListPagerAdapter(getSupportFragmentManager(), this);
		adapter.addTab(GeekListDescriptionFragment.newInstance(), R.string.title_description, new ItemInstantiatedCallback() {
			@Override
			public void itemInstantiated(String tag) {
				descriptionFragmentTag = tag;
				setDescription();
			}
		});
		adapter.addTab(GeekListItemsFragment.newInstance(), R.string.title_items, new ItemInstantiatedCallback() {
			@Override
			public void itemInstantiated(String tag) {
				itemsFragmentTag = tag;
				setItems();
			}
		});
		return adapter;
	}

	private interface ItemInstantiatedCallback {
		void itemInstantiated(String tag);
	}

	private final static class GeekListPagerAdapter extends FragmentPagerAdapter {
		static final class TabInfo {
			private final Fragment fragment;
			@StringRes private final int titleRes;
			private final ItemInstantiatedCallback callback;

			TabInfo(Fragment fragment, int titleRes, ItemInstantiatedCallback callback) {
				this.fragment = fragment;
				this.titleRes = titleRes;
				this.callback = callback;
			}
		}

		private final Context context;
		private final ArrayList<TabInfo> tabs = new ArrayList<>();

		public GeekListPagerAdapter(FragmentManager fragmentManager, Context context) {
			super(fragmentManager);
			this.context = context;
			tabs.clear();
		}

		public void addTab(Fragment fragment, @StringRes int titleRes, ItemInstantiatedCallback callback) {
			tabs.add(new TabInfo(fragment, titleRes, callback));
			notifyDataSetChanged();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return "";
			return context.getString(tabInfo.titleRes);
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return null;
			return tabInfo.fragment;
		}

		@Override
		@NonNull
		public Object instantiateItem(ViewGroup container, int position) {
			Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo != null) {
				tabInfo.callback.itemInstantiated(createdFragment.getTag());
			}
			return createdFragment;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}
	}

	@Override
	@NonNull
	public Loader<SafeResponse<GeekListResponse>> onCreateLoader(int id, Bundle data) {
		return new GeekListLoader(this, geekListId);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<SafeResponse<GeekListResponse>> loader, SafeResponse<GeekListResponse> data) {
		GeekListResponse body = data.getBody();
		if (body == null) {
			errorMessage = getString(R.string.empty_geeklist);
		} else if (data.hasParseError()) {
			errorMessage = getString(R.string.parse_error);
		} else if (data.hasError()) {
			errorMessage = data.getErrorMessage();
		} else {
			errorMessage = "";
		}

		if (body == null) return;

		geekList = new GeekList(
			body.id,
			TextUtils.isEmpty(body.title) ? "" : body.title.trim(),
			body.username,
			body.description,
			StringUtils.parseInt(body.numitems),
			StringUtils.parseInt(body.thumbs),
			DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.postdate, GeekListResponse.FORMAT),
			DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.editdate, GeekListResponse.FORMAT)
		);
		geekListItems = body.getItems();

		setDescription();
		setItems();
	}

	private void setDescription() {
		if (adapter == null) return;
		GeekListDescriptionFragment descriptionFragment = (GeekListDescriptionFragment) getSupportFragmentManager().findFragmentByTag(descriptionFragmentTag);
		if (descriptionFragment != null) descriptionFragment.setData(geekList);
	}

	private void setItems() {
		if (geekList == null || geekListItems == null) return;
		if (adapter == null) return;

		GeekListItemsFragment itemsFragment = (GeekListItemsFragment) getSupportFragmentManager().findFragmentByTag(itemsFragmentTag);
		if (itemsFragment != null) {
			if (!TextUtils.isEmpty(errorMessage)) {
				itemsFragment.setError(errorMessage);
			} else if (geekList.getNumberOfItems() == 0 || geekListItems.size() == 0) {
				itemsFragment.setError();
			} else {
				itemsFragment.setData(geekList, geekListItems);
			}
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<SafeResponse<GeekListResponse>> loader) {
	}

	private static class GeekListLoader extends BggLoader<SafeResponse<GeekListResponse>> {
		private final BggService service;
		private final int geekListId;

		public GeekListLoader(Context context, int geekListId) {
			super(context);
			service = Adapter.createForXml();
			this.geekListId = geekListId;
		}

		@Override
		public SafeResponse<GeekListResponse> loadInBackground() {
			return new SafeResponse<>(service.geekList(geekListId, 1));
		}
	}
}
