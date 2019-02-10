package com.boardgamegeek.util;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.transition.Transition;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

import com.boardgamegeek.R;

import static android.view.View.GONE;

/**
 * Helper class for animations.
 */
public class AnimationUtils {
	private AnimationUtils() {
	}

	public static void slideUpIn(final View view) {
		if (view == null || view.getVisibility() == View.VISIBLE) return;
		final Animation animation = android.view.animation.AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_up);
		view.startAnimation(animation);
		view.setVisibility(View.VISIBLE);
	}

	public static void slideDownOut(final View view) {
		if (view == null || view.getVisibility() == View.GONE) return;
		final Animation animation = android.view.animation.AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_down);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
		view.startAnimation(animation);
	}

	public static void fadeIn(final View view) {
		if (view == null || view.getVisibility() == View.VISIBLE) return;
		fadeIn(view.getContext(), view, true);
	}

	public static void fadeIn(final View view, boolean animate) {
		if (view == null || view.getVisibility() == View.VISIBLE) return;
		fadeIn(view.getContext(), view, animate);
	}

	public static void fadeIn(Context context, final View view, boolean animate) {
		if (view == null || view.getVisibility() == View.VISIBLE) return;
		if (animate) {
			final Animation animation = android.view.animation.AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
			view.startAnimation(animation);
		} else {
			view.clearAnimation();
		}
		view.setVisibility(View.VISIBLE);
	}

	public static void fadeOut(final View view) {
		if (view == null || view.getVisibility() != View.VISIBLE) return;
		fadeOut(view.getContext(), view, true, GONE);
	}

	private static void fadeOut(Context context, final View view, boolean animate, final int visibility) {
		if (view == null || view.getVisibility() != View.VISIBLE) return;
		if (animate) {
			final Animation animation = android.view.animation.AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
			animation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					view.setVisibility(visibility);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}
			});
			view.startAnimation(animation);
		} else {
			view.clearAnimation();
			view.setVisibility(GONE);
		}
	}

	public static void setInterpolator(Context context, Transition transition) {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			transition.setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in));
		}
	}
}
