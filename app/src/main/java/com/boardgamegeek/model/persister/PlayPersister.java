package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PlayPersister {
	private final Context context;
	private final ContentResolver resolver;
	private final ArrayList<ContentProviderOperation> batch;

	public PlayPersister(Context context) {
		this.context = context;
		resolver = context.getContentResolver();
		batch = new ArrayList<>();
	}

	public long save(Play play, long internalId, boolean includePlayers) {
		if (play == null) return BggContract.INVALID_ID;
		if (!isBoardgameSubtype(play)) return BggContract.INVALID_ID;

		batch.clear();
		ContentValues values = createContentValues(play);

		String debugMessage;
		if (internalId != BggContract.INVALID_ID) {
			debugMessage = "Updating play _ID " + internalId;
			batch.add(ContentProviderOperation
				.newUpdate(Plays.buildPlayUri(internalId))
				.withValues(values)
				.build());
		} else if (play.deleteTimestamp > 0) {
			Timber.i("Skipping inserting a deleted play");
			return BggContract.INVALID_ID;
		} else {
			debugMessage = "Inserting new play";
			batch.add(ContentProviderOperation
				.newInsert(Plays.CONTENT_URI)
				.withValues(values)
				.build());
		}

		if (includePlayers) {
			deletePlayerWithEmptyUserNameInBatch(internalId);
			List<String> existingPlayerIds = removeDuplicateUserNamesFromBatch(internalId);
			addPlayersToBatch(play, existingPlayerIds, internalId);
			removeUnusedPlayersFromBatch(internalId, existingPlayerIds);

			if (play.playId > 0 || play.updateTimestamp > 0) {
				saveGamePlayerSortOrderToBatch(play);
				updateColorsInBatch(play);
				saveBuddyNicknamesToBatch(play);
			}
		}

		ContentProviderResult[] results = ResolverUtils.applyBatch(context, batch, debugMessage);
		long insertedId = internalId;
		if (insertedId == BggContract.INVALID_ID && results != null && results.length > 0) {
			insertedId = StringUtils.parseLong(results[0].uri.getLastPathSegment(), BggContract.INVALID_ID);
		}
		Timber.i("Saved play _ID=%s", insertedId);
		return insertedId;
	}

	private boolean isBoardgameSubtype(Play play) {
		if (play.subtypes == null || play.subtypes.isEmpty()) {
			return true;
		}
		for (String subtype : play.subtypes) {
			if (subtype.startsWith("boardgame")) {
				return true;
			}
		}
		return false;
	}

	private static int generateSyncHashCode(Play play) {
		StringBuilder sb = new StringBuilder();
		sb.append(play.getDateForDatabase()).append("\n");
		sb.append(play.quantity).append("\n");
		sb.append(play.length).append("\n");
		sb.append(play.incomplete).append("\n");
		sb.append(play.noWinStats).append("\n");
		sb.append(play.location).append("\n");
		sb.append(play.comments).append("\n");
		for (Player player : play.getPlayers()) {
			sb.append(player.username).append("\n");
			sb.append(player.userId).append("\n");
			sb.append(player.name).append("\n");
			sb.append(player.startingPosition).append("\n");
			sb.append(player.color).append("\n");
			sb.append(player.score).append("\n");
			sb.append(player.isNew).append("\n");
			sb.append(player.rating).append("\n");
			sb.append(player.isWin).append("\n");
		}
		return sb.toString().hashCode();
	}

	private static ContentValues createContentValues(Play play) {
		ContentValues values = new ContentValues();
		values.put(Plays.PLAY_ID, play.playId);
		values.put(Plays.DATE, play.getDateForDatabase());
		values.put(Plays.ITEM_NAME, play.gameName);
		values.put(Plays.OBJECT_ID, play.gameId);
		values.put(Plays.QUANTITY, play.quantity);
		values.put(Plays.LENGTH, play.length);
		values.put(Plays.INCOMPLETE, play.incomplete);
		values.put(Plays.NO_WIN_STATS, play.noWinStats);
		values.put(Plays.LOCATION, play.location);
		values.put(Plays.COMMENTS, play.comments);
		values.put(Plays.PLAYER_COUNT, play.getPlayerCount());
		values.put(Plays.SYNC_TIMESTAMP, play.syncTimestamp);
		values.put(Plays.START_TIME, play.length > 0 ? 0 : play.startTime); // only store start time if there's no length
		values.put(Plays.SYNC_HASH_CODE, generateSyncHashCode(play));
		values.put(Plays.DELETE_TIMESTAMP, play.deleteTimestamp);
		values.put(Plays.UPDATE_TIMESTAMP, play.updateTimestamp);
		values.put(Plays.DIRTY_TIMESTAMP, play.dirtyTimestamp);
		return values;
	}

	private void deletePlayerWithEmptyUserNameInBatch(long internalId) {
		if (internalId == BggContract.INVALID_ID) return;
		batch.add(ContentProviderOperation
			.newDelete(Plays.buildPlayerUri(internalId))
			.withSelection(String.format("%1$s IS NULL OR %1$s=''", PlayPlayers.USER_NAME), null)
			.build());
	}

	private List<String> removeDuplicateUserNamesFromBatch(long internalId) {
		if (internalId == BggContract.INVALID_ID) return new ArrayList<>(0);
		List<String> userNames = ResolverUtils.queryStrings(resolver, Plays.buildPlayerUri(internalId), PlayPlayers.USER_NAME);

		if (userNames == null || userNames.size() == 0) {
			return new ArrayList<>();
		}

		List<String> uniqueUserNames = new ArrayList<>();
		List<String> userNamesToDelete = new ArrayList<>();

		for (int i = 0; i < userNames.size(); i++) {
			String userName = userNames.get(i);
			if (!TextUtils.isEmpty(userName)) {
				if (uniqueUserNames.contains(userName)) {
					userNamesToDelete.add(userName);
				} else {
					uniqueUserNames.add(userName);
				}
			}
		}

		for (String userName : userNamesToDelete) {
			batch.add(ContentProviderOperation
				.newDelete(Plays.buildPlayerUri(internalId))
				.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { userName })
				.build());
			uniqueUserNames.remove(userName);
		}

		return uniqueUserNames;
	}

	private void addPlayersToBatch(Play play, List<String> playerUserNames, long internalId) {
		for (Player player : play.getPlayers()) {
			String userName = player.username;
			ContentValues values = new ContentValues();
			values.put(PlayPlayers.USER_ID, player.userId);
			values.put(PlayPlayers.USER_NAME, userName);
			values.put(PlayPlayers.NAME, player.name);
			values.put(PlayPlayers.START_POSITION, player.startingPosition);
			values.put(PlayPlayers.COLOR, player.color);
			values.put(PlayPlayers.SCORE, player.score);
			values.put(PlayPlayers.NEW, player.isNew);
			values.put(PlayPlayers.RATING, player.rating);
			values.put(PlayPlayers.WIN, player.isWin);

			if (playerUserNames != null && playerUserNames.remove(userName)) {
				batch.add(ContentProviderOperation
					.newUpdate(Plays.buildPlayerUri(internalId))
					.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { userName })
					.withValues(values).build());
			} else {
				values.put(PlayPlayers.USER_NAME, userName);
				if (internalId == BggContract.INVALID_ID) {
					batch.add(ContentProviderOperation
						.newInsert(Plays.buildPlayerUri())
						.withValueBackReference(PlayPlayers._PLAY_ID, 0)
						.withValues(values)
						.build());
				} else {
					batch.add(ContentProviderOperation
						.newInsert(Plays.buildPlayerUri(internalId))
						.withValues(values)
						.build());
				}
			}
		}
	}

	private void removeUnusedPlayersFromBatch(long internalId, List<String> playerUserNames) {
		if (internalId == BggContract.INVALID_ID) return;
		if (playerUserNames == null) return;
		for (String playerUserName : playerUserNames) {
			batch.add(ContentProviderOperation
				.newDelete(Plays.buildPlayerUri(internalId))
				.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { playerUserName })
				.build());
		}
	}

	/**
	 * Determine if the players are custom sorted or not, and save it to the game.
	 */
	private void saveGamePlayerSortOrderToBatch(Play play) {
		// We can't determine the sort order without players
		if (play.getPlayerCount() == 0) return;

		// We can't save the sort order if we aren't storing the game
		Uri gameUri = Games.buildGameUri(play.gameId);
		if (!ResolverUtils.rowExists(resolver, gameUri)) return;

		batch.add(ContentProviderOperation
			.newUpdate(gameUri)
			.withValue(Games.CUSTOM_PLAYER_SORT, play.arePlayersCustomSorted())
			.build());
	}

	/**
	 * Add the current players' team/colors to the permanent list for the game.
	 */
	private void updateColorsInBatch(Play play) {
		// There are no players, so there are no colors to save
		if (play.getPlayerCount() == 0) return;

		// We can't save the colors if we aren't storing the game
		if (!ResolverUtils.rowExists(resolver, Games.buildGameUri(play.gameId))) return;

		Uri insertUri = Games.buildColorsUri(play.gameId);
		List<String> insertedColors = new ArrayList<>();

		for (Player player : play.getPlayers()) {
			String color = player.color;
			if (!TextUtils.isEmpty(color) &&
				!insertedColors.contains(color) &&
				!ResolverUtils.rowExists(resolver, Games.buildColorsUri(play.gameId, color))) {
				batch.add(ContentProviderOperation
					.newInsert(insertUri)
					.withValue(GameColors.COLOR, color)
					.build());
				insertedColors.add(color);
			}
		}
	}

	/**
	 * Update GeekBuddies' nicknames with the names used here.
	 */
	private void saveBuddyNicknamesToBatch(Play play) {
		// There are no players, so there are no nicknames to save
		if (play.getPlayerCount() == 0) return;

		for (Player player : play.getPlayers()) {
			if (!TextUtils.isEmpty(player.username) && !TextUtils.isEmpty(player.name)) {
				batch.add(ContentProviderOperation
					.newUpdate(Buddies.CONTENT_URI)
					.withSelection(Buddies.BUDDY_NAME + "=?", new String[] { player.username })
					.withValue(Buddies.PLAY_NICKNAME, player.name)
					.build());
			}
		}
	}
}
