package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.AverageWeightFilterer;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;

public class AverageWeightFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;
	private static final DecimalFormat FORMAT = new DecimalFormat("#.0");

	@Override
	protected int getAbsoluteMax() {
		return (int) (AverageWeightFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (AverageWeightFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	public int getType(Context context) {
		return new AverageWeightFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new AverageWeightFilterer(context, (double) (min) / FACTOR, (double) (max) / FACTOR, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_average_weight;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_average_weight;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		double min = AverageWeightFilterer.MIN_RANGE;
		double max = AverageWeightFilterer.MAX_RANGE;
		boolean includeUndefined = false;
		if (filter != null) {
			AverageWeightFilterer data = (AverageWeightFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUndefined = data.includeUndefined();
		}
		return new InitialValues((int) (min * FACTOR), (int) (max * FACTOR), includeUndefined);
	}

	@Override
	protected String getPinText(String value) {
		return FORMAT.format((double) StringUtils.parseInt(value, 0) / FACTOR);
	}
}
