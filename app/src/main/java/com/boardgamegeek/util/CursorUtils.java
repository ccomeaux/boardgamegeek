package com.boardgamegeek.util;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import timber.log.Timber;

/**
 * Static methods for getting data out of a cursor.
 */
public class CursorUtils {
	private CursorUtils() {
	}

	/**
	 * Gets a boolean from the specified column on the current row of the cursor. Returns false if the column doesn't exist.
	 */
	public static boolean getBoolean(Cursor cursor, String columnName) {
		return getBoolean(cursor, columnName, false);
	}

	/**
	 * Gets a boolean from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static boolean getBoolean(Cursor cursor, String columnName, boolean defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		return getBoolean(cursor, idx, defaultValue);
	}

	public static boolean getBoolean(Cursor cursor, int idx) {
		return getBoolean(cursor, idx, false);
	}

	public static boolean getBoolean(Cursor cursor, int idx, boolean defaultValue) {
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx) != 0;
		}
	}

	/**
	 * Gets an integer from the specified column on the current row of the cursor. Returns 0 if the column doesn't exist.
	 */
	public static int getInt(Cursor cursor, String columnName) {
		return getInt(cursor, columnName, 0);
	}

	/**
	 * Gets an integer from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static int getInt(Cursor cursor, String columnName, int defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx);
		}
	}

	/**
	 * Gets a long from the specified column on the current row of the cursor. Returns 0 if the column doesn't exist.
	 */
	public static long getLong(Cursor cursor, String columnName) {
		return getLong(cursor, columnName, 0);
	}

	/**
	 * Gets a long from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static long getLong(Cursor cursor, String columnName, long defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getLong(idx);
		}
	}

	/**
	 * Gets a double from the specified column on the current row of the cursor. Returns 0.0 if the column doesn't exist.
	 */
	public static double getDouble(Cursor cursor, String columnName) {
		return getDouble(cursor, columnName, 0.0);
	}

	/**
	 * Gets a double from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static double getDouble(Cursor cursor, String columnName, double defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getDouble(idx);
		}
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns an empty string if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, String columnName) {
		return getString(cursor, columnName, "");
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns an empty string if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, int columnIndex) {
		return getString(cursor, columnIndex, "");
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, String columnName, String defaultValue) {
		return getString(cursor, cursor.getColumnIndex(columnName), defaultValue);
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, int columnIndex, String defaultValue) {
		if (columnIndex == -1) {
			return defaultValue;
		} else {
			String value = cursor.getString(columnIndex);
			if (value == null) {
				return defaultValue;
			}
			return value;
		}
	}

	/**
	 * Gets string array from the cursor based on the columnIndex. Returns with the cursor at the original position.
	 */
	public static String[] getStringArray(Cursor cursor, int columnIndex) {
		String[] array = new String[cursor.getCount()];
		int position = cursor.getPosition();
		try {
			cursor.moveToPosition(-1);
			int i = 0;
			while (cursor.moveToNext()) {
				array[i++] = cursor.getString(columnIndex);
			}
		} finally {
			cursor.moveToPosition(position);
		}
		return array;
	}

	/**
	 * Gets a date from the specified column on the current row of the cursor. The date is formatted as an abbreviated
	 * form of the local conventions. Returns an empty string if the date could not be determined.
	 */
	public static String getFormattedDateAbbreviated(Cursor cursor, Context context, int columnIndex) {
		return getFormattedDate(cursor, context, columnIndex, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
	}

	/**
	 * Gets a date from the specified column on the current row of the cursor. The date is formatted according to the
	 * specified flags. Returns an empty string if the date could not be determined.
	 */
	private static String getFormattedDate(Cursor cursor, Context context, int columnIndex, int flags) {
		String date = cursor.getString(columnIndex);
		if (!TextUtils.isEmpty(date)) {
			try {
				Calendar calendar = getCalendar(date);
				return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), flags);
			} catch (Exception e) {
				Timber.e(e, "Could find a date in here: %s", date);
			}
		}
		return "";
	}

	@NonNull
	private static Calendar getCalendar(String date) {
		Timber.v("Getting date from string: %s", date);
		String[] dateParts = date.split("-");
		int year = Integer.parseInt(dateParts[0]);
		int month = Integer.parseInt(dateParts[1]) - 1;
		int day = Integer.parseInt(dateParts[2]);
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);
		return calendar;
	}

	public static String getFirstCharacter(Cursor cursor, int position, String columnName, String defaultValue) {
		if (cursor == null) {
			return defaultValue;
		}

		int columnIndex = cursor.getColumnIndex(columnName);
		if (columnIndex == -1) {
			return defaultValue;
		}

		int cur = cursor.getPosition();
		try {
			String value = null;
			cursor.moveToPosition(position);
			if (columnIndex < cursor.getColumnCount()) {
				value = cursor.getString(columnIndex);
			}
			if (TextUtils.isEmpty(value)) {
				return defaultValue;
			}

			return value.substring(0, 1).toUpperCase(Locale.getDefault());
		} finally {
			cursor.moveToPosition(cur);
		}
	}
}
