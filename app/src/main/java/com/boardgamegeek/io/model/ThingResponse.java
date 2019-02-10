package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class ThingResponse {
	public static final int PAGE_SIZE = 100;

	@ElementList(name = "items", inline = true, required = false)
	private List<Game> games;

	@NonNull
	public List<Game> getGames() {
		if (games == null) {
			return new ArrayList<>();
		}
		return games;
	}
}
