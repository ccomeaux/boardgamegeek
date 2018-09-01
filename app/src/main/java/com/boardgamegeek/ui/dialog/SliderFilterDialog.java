package com.boardgamegeek.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.appyvet.materialrangebar.RangeBar;
import com.appyvet.materialrangebar.RangeBar.OnRangeBarChangeListener;
import com.appyvet.materialrangebar.RangeBar.PinTextFormatter;
import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.util.MathUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public abstract class SliderFilterDialog implements CollectionFilterDialog {
	private Integer low;
	private Integer high;
	@BindView(R.id.explanation) TextView explanationView;
	@BindView(R.id.checkbox) CheckBox checkBox;
	@BindView(R.id.range_bar) RangeBar rangeBar;

	public void createDialog(final Context context, final OnFilterChangedListener listener, CollectionFilterer filter) {
		LayoutInflater inflater = LayoutInflater.from(context);
		@SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_slider_filter, null);
		ButterKnife.bind(this, layout);

		InitialValues initialValues = initValues(filter);
		low = MathUtils.constrain(initialValues.min, getAbsoluteMin(), getAbsoluteMax());
		high = MathUtils.constrain(initialValues.max, getAbsoluteMin(), getAbsoluteMax());

		initSlider();

		//noinspection ResourceType
		checkBox.setVisibility(getCheckboxVisibility());
		checkBox.setText(getCheckboxTextId());
		checkBox.setChecked(initialValues.isChecked);

		initExplanation();

		AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(getTitleId())
			.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) listener.removeFilter(getType(context));
				}
			})
			.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if (listener != null) listener.addFilter(getPositiveData(context, low, high, checkBox.isChecked()));
				}
			})
			.setView(layout);

		builder.create().show();
	}

	private void initSlider() {
		rangeBar.setTickStart(getAbsoluteMin());
		rangeBar.setTickEnd(getAbsoluteMax());
		rangeBar.setRangePinsByValue(low, high);

		rangeBar.setPinTextFormatter(new PinTextFormatter() {
			@Override
			public String getText(String value) {
				return getPinText(value);
			}
		});

		rangeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
			@Override
			public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
				low = MathUtils.constrain(leftPinIndex + getAbsoluteMin(), getAbsoluteMin(), getAbsoluteMax());
				high = MathUtils.constrain(rightPinIndex + getAbsoluteMin(), getAbsoluteMin(), getAbsoluteMax());
			}
		});
	}

	@OnClick(R.id.min_up)
	public void onMinUpClick() {
		if (rangeBar.getLeftIndex() < rangeBar.getTickCount() - 1) {
			if (rangeBar.getLeftIndex() == rangeBar.getRightIndex()) {
				updateRange(rangeBar.getLeftIndex() + 1, rangeBar.getRightIndex() + 1);
			} else {
				updateRange(rangeBar.getLeftIndex() + 1, rangeBar.getRightIndex());
			}
		}
	}

	@OnClick(R.id.min_down)
	public void onMinDownClick() {
		if (rangeBar.getLeftIndex() > 0) {
			updateRange(rangeBar.getLeftIndex() - 1, rangeBar.getRightIndex());
		}
	}

	@OnClick(R.id.max_up)
	public void onMaxUpClick() {
		if (rangeBar.getRightIndex() < rangeBar.getTickCount() - 1) {
			updateRange(rangeBar.getLeftIndex(), rangeBar.getRightIndex() + 1);
		}
	}

	@OnClick(R.id.max_down)
	public void onMaxDownClick() {
		if (rangeBar.getRightIndex() > 0) {
			if (rangeBar.getLeftIndex() == rangeBar.getRightIndex()) {
				updateRange(rangeBar.getLeftIndex() - 1, rangeBar.getRightIndex() - 1);
			} else {
				updateRange(rangeBar.getLeftIndex(), rangeBar.getRightIndex() - 1);
			}
		}
	}

	private void updateRange(int leftPinIndex, int rightPinIndex) {
		int left = MathUtils.constrain(leftPinIndex, 0, rangeBar.getTickCount() - 1);
		int right = MathUtils.constrain(rightPinIndex, 0, rangeBar.getTickCount() - 1);
		rangeBar.setRangePinsByIndices(left, right);
		// HACK to make the pins remain visible
		rangeBar.setLeft(rangeBar.getLeft() + 1);
	}

	private void initExplanation() {
		if (getDescriptionId() == -1) {
			explanationView.setVisibility(View.GONE);
		} else {
			explanationView.setText(getDescriptionId());
			explanationView.setVisibility(View.VISIBLE);
		}
	}

	protected abstract InitialValues initValues(CollectionFilterer filter);

	@StringRes
	protected abstract int getTitleId();

	protected abstract CollectionFilterer getPositiveData(final Context context, int min, int max, boolean checkbox);

	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	@StringRes
	protected int getCheckboxTextId() {
		return R.string.include_missing_values;
	}

	@StringRes
	protected int getDescriptionId() {
		return -1;
	}

	protected abstract int getAbsoluteMin();

	protected abstract int getAbsoluteMax();

	protected String getPinText(String value) {
		return value;
	}

	class InitialValues {
		final int min;
		final int max;
		final boolean isChecked;

		InitialValues(int min, int max) {
			this.min = min;
			this.max = max;
			this.isChecked = false;
		}

		InitialValues(int min, int max, boolean isChecked) {
			this.min = min;
			this.max = max;
			this.isChecked = isChecked;
		}
	}
}
