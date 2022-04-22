package com.boardgamegeek.mappers

import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.*
import com.boardgamegeek.util.ForumXmlApiMarkupConverter
import java.text.SimpleDateFormat
import java.util.*

fun Forum.mapToEntity() = ForumEntity(
    id = this.id,
    title = this.title,
    numberOfThreads = this.numthreads,
    lastPostDateTime = this.lastpostdate.toMillis(dateFormat),
    isHeader = this.noposting == 1,
)

fun ForumListResponse.mapToEntity() = this.forums.map { it.mapToEntity() }

fun ForumThread.mapToEntity() = ThreadEntity(
    threadId = this.id,
    subject = this.subject.orEmpty().trim(),
    author = this.author.orEmpty().trim(),
    numberOfArticles = this.numarticles,
    lastPostDate = this.lastpostdate.toMillis(dateFormat),
)

fun List<ForumThread>.mapToEntity() = this.map { it.mapToEntity() }

fun ForumResponse.mapToEntity() = ForumThreadsEntity(
    numberOfThreads = this.numthreads.toIntOrNull() ?: 0,
    threads = this.threads.orEmpty().mapToEntity(),
)

fun ArticleElement.mapToEntity(converter: ForumXmlApiMarkupConverter) = ArticleEntity(
    id = this.id,
    username = this.username.orEmpty(),
    link = this.link,
    postTicks = this.postdate.toMillis(dateFormat2),
    editTicks = this.editdate.toMillis(dateFormat2),
    body = converter.toHtml(this.body?.trim().orEmpty()),
    numberOfEdits = this.numedits,
)

fun ThreadResponse.mapToEntity(converter: ForumXmlApiMarkupConverter) = ThreadArticlesEntity(
    threadId = this.id,
    subject = this.subject,
    articles = this.articles.map { it.mapToEntity(converter) }
)

private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

@Suppress("SpellCheckingInspection")
private val dateFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US)
