package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.MyRatingFilterer;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;

public class MyRatingFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;
	private static final DecimalFormat FORMAT = new DecimalFormat("#.0");

	@Override
	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	@Override
	protected int getCheckboxTextId() {
		return R.string.unrated;
	}

	@Override
	protected int getAbsoluteMax() {
		return (int) (MyRatingFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (MyRatingFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	public int getType(Context context) {
		return new MyRatingFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new MyRatingFilterer(context, (double) (min) / FACTOR, (double) (max) / FACTOR, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_my_rating;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		double min = MyRatingFilterer.MIN_RANGE;
		double max = MyRatingFilterer.MAX_RANGE;
		boolean includeUnrated = true;
		if (filter != null) {
			MyRatingFilterer data = (MyRatingFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUnrated = data.includeUnrated();
		}
		return new InitialValues((int) (min * FACTOR), (int) (max * FACTOR), includeUnrated);
	}

	@Override
	protected String getPinText(String value) {
		return FORMAT.format((double) StringUtils.parseInt(value, 0) / FACTOR);
	}
}