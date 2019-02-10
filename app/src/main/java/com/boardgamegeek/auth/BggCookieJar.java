package com.boardgamegeek.auth;

import android.text.TextUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import timber.log.Timber;

public class BggCookieJar implements CookieJar {
	private String authToken;
	private long authTokenExpiry;

	@Override
	public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
		Timber.w(url.toString());
		Timber.w(cookies.toString());
		for (Cookie cookie : cookies) {
			if ("bggpassword".equalsIgnoreCase(cookie.name())) {
				authToken = cookie.value();
				authTokenExpiry = cookie.expiresAt();
			}
		}
	}

	@Override
	public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
		return Collections.emptyList();
	}

	public boolean isValid() {
		return !TextUtils.isEmpty(authToken);
	}

	@NonNull
	public static BggCookieJar getMock() {
		BggCookieJar authResponse = new BggCookieJar();
		authResponse.authToken = "password";
		authResponse.authTokenExpiry = Long.MAX_VALUE;
		return authResponse;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("token: %s (%s)", authToken, new Date(authTokenExpiry));
	}

	public String getAuthToken() {
		return authToken;
	}

	public long getAuthTokenExpiry() {
		return authTokenExpiry;
	}
}
