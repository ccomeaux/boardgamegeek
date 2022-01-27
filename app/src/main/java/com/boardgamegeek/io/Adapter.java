package com.boardgamegeek.io;

import android.content.Context;

import com.boardgamegeek.util.HttpUtils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class Adapter {
	public static GeekdoApi createGeekdoApi() {
		return new Retrofit.Builder()
			.client(HttpUtils.getHttpClient(true))
			.baseUrl("https://api.geekdo.com")
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(GeekdoApi.class);
	}

	public static BggService createForXml() {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(null);
		builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
		return builder.build().create(BggService.class);
	}

	public static BggService createForXmlWithAuth(Context context) {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(context);
		builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
		return builder.build().create(BggService.class);
	}

	public static BggService createForJson() {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(null);
		builder.addConverterFactory(GsonConverterFactory.create());
		return builder.build().create(BggService.class);
	}

	private static Retrofit.Builder createBuilderWithoutConverterFactory(Context context) {
		okhttp3.OkHttpClient httpClient;
		if (context == null) {
			httpClient = HttpUtils.getHttpClient(true);
		} else {
			httpClient = HttpUtils.getHttpClientWithAuth(context);
		}
		return new Retrofit.Builder()
			.baseUrl("https://boardgamegeek.com/")
			.client(httpClient);
	}
}
