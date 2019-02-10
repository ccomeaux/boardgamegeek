package com.boardgamegeek.ui.adapter;


import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.ui.ThreadActivity;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.ui.widget.TimestampView;

import java.text.NumberFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForumRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<Thread> {
	private final int forumId;
	private final String forumTitle;
	private final int gameId;
	private final String gameName;
	private final NumberFormat numberFormat;

	public ForumRecyclerViewAdapter(Context context, PaginatedData<Thread> data, int forumId, String forumTitle, int gameId, String gameName) {
		super(context, R.layout.row_forum_thread, data);
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.gameId = gameId;
		this.gameName = gameName;
		numberFormat = NumberFormat.getNumberInstance();
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new ThreadViewHolder(itemView);
	}

	public class ThreadViewHolder extends PaginatedItemViewHolder {
		@BindView(R.id.subject) TextView subjectView;
		@BindView(R.id.author) TextView authorView;
		@BindView(R.id.number_of_articles) TextView numberOfArticlesView;
		@BindView(R.id.last_post_date) TimestampView lastPostDateView;

		public ThreadViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}

		@Override
		protected void bind(final Thread thread) {
			final Context context = itemView.getContext();
			subjectView.setText(thread.subject.trim());
			authorView.setText(thread.author);
			int replies = thread.numberOfArticles - 1;
			numberOfArticlesView.setText(numberFormat.format(replies));
			lastPostDateView.setTimestamp(thread.lastPostDate());
			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ThreadActivity.start(context, thread.id, thread.subject, forumId, forumTitle, gameId, gameName);
				}
			});
		}
	}
}
