@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.*
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.text.SimpleDateFormat
import java.util.*

interface BggService {
    @GET("/xmlapi2/collection")
    fun collection(@Query("username") username: String?, @QueryMap options: Map<String, String>): Call<CollectionResponse>

    @GET("/xmlapi2/collection")
    suspend fun collectionC(@Query("username") username: String?, @QueryMap options: Map<String, String>): CollectionResponse

    @GET("/xmlapi2/thing")
    fun thing(@Query("id") gameId: Int, @Query("stats") stats: Int): Call<ThingResponse>

    @GET("/xmlapi2/thing")
    suspend fun thing2(@Query("id") gameId: Int, @Query("stats") stats: Int): ThingResponse

    @GET("/xmlapi2/thing")
    fun thing(@Query("id") gameIds: String?, @Query("stats") stats: Int): Call<ThingResponse>

    @GET("/xmlapi2/thing?comments=1")
    suspend fun thingWithComments(@Query("id") gameId: Int, @Query("page") page: Int): ThingResponse

    @GET("/xmlapi2/thing?ratingcomments=1")
    suspend fun thingWithRatings(@Query("id") gameId: Int, @Query("page") page: Int): ThingResponse

    @GET("/xmlapi2/plays")
    suspend fun playsByDate(@Query("username") username: String?, @Query("mindate") minDate: String?, @Query("maxdate") maxDate: String?, @Query("page") page: Int): PlaysResponse

    @GET("/xmlapi2/plays")
    suspend fun playsByGame(@Query("username") username: String?, @Query("id") gameId: Int, @Query("page") page: Int): PlaysResponse

    @GET("/xmlapi2/plays")
    suspend fun plays(@Query("username") username: String?, @Query("mindate") minDate: String?, @Query("maxdate") maxDate: String?, @Query("page") page: Int): PlaysResponse

    @GET("/xmlapi2/user")
    suspend fun user(@Query("name") name: String?): User

    @GET("/xmlapi2/user")
    suspend fun user(@Query("name") name: String?, @Query("buddies") buddies: Int, @Query("page") page: Int): User

    @GET("/xmlapi/{type}/{id}")
    suspend fun person(@Path("type") type: String?, @Path("id") id: Int): Person

    @GET("/xmlapi2/person")
    suspend fun person(@Query("id") id: Int): PersonResponse

    @GET("/xmlapi2/company/{id}")
    suspend fun company(@Query("id") id: Int): CompanyResponse

    @GET("/xmlapi2/search")
    suspend fun search(@Query("query") query: String?, @Query("type") type: String?, @Query("exact") exact: Int): SearchResponse

    @GET("/xmlapi2/hot")
    suspend fun getHotness(@Query("type") type: String?): HotnessResponse

    @GET("/xmlapi2/forumlist")
    suspend fun forumList(@Query("type") type: String?, @Query("id") id: Int): ForumListResponse

    @GET("/xmlapi2/forum")
    suspend fun forum(@Query("id") id: Int, @Query("page") page: Int): ForumResponse

    @GET("/xmlapi2/thread")
    suspend fun thread(@Query("id") id: Int): ThreadResponse

    @GET("/geeklist/module?ajax=1&domain=boardgame&nosession=1&tradelists=0&version=v5")
    suspend fun geekLists(@Query("sort") sort: String?, @Query("showcount") pageSize: Int, @Query("pageid") page: Int): GeekListsResponse

    @GET("/xmlapi/geeklist/{id}")
    suspend fun geekList(@Path("id") id: Int, @Query("comments") comments: Int): GeekListResponse

    companion object {
        const val THING_SUBTYPE_BOARDGAME = "boardgame"
        const val THING_SUBTYPE_BOARDGAME_EXPANSION = "boardgameexpansion"
        const val THING_SUBTYPE_BOARDGAME_ACCESSORY = "boardgameaccessory"

        const val RANK_TYPE_SUBTYPE = "subtype"
        const val RANK_TYPE_FAMILY = "family"

        const val RANK_FAMILY_NAME_ABSTRACT_GAMES = "abstracts"
        const val RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES = "cgs"
        const val RANK_FAMILY_NAME_CHILDRENS_GAMES = "childrensgames"
        const val RANK_FAMILY_NAME_FAMILY_GAMES = "familygames"
        const val RANK_FAMILY_NAME_PARTY_GAMES = "partygames"
        const val RANK_FAMILY_NAME_STRATEGY_GAMES = "strategygames"
        const val RANK_FAMILY_NAME_THEMATIC_GAMES = "thematic"
        const val RANK_FAMILY_NAME_WAR_GAMES = "wargames"

        const val COLLECTION_QUERY_KEY_ID = "id"
        const val COLLECTION_QUERY_KEY_SHOW_PRIVATE = "showprivate"
        const val COLLECTION_QUERY_KEY_STATS = "stats"
        const val COLLECTION_QUERY_KEY_MODIFIED_SINCE = "modifiedsince"
        const val COLLECTION_QUERY_KEY_BRIEF = "brief"
        const val COLLECTION_QUERY_KEY_SUBTYPE = "subtype"
        const val COLLECTION_QUERY_STATUS_PLAYED = "played"

        // val COLLECTION_QUERY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val COLLECTION_QUERY_DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        const val PERSON_TYPE_ARTIST = "boardgameartist"
        const val PERSON_TYPE_DESIGNER = "boardgamedesigner"

        const val SEARCH_TYPE_BOARD_GAME = "boardgame"
        // const val SEARCH_TYPE_BOARD_GAME_EXPANSION = "boardgameexpansion"
        // const val SEARCH_TYPE_RPG = "rpg"
        // const val SEARCH_TYPE_RPG_ITEM = "rpgitem"
        // const val SEARCH_TYPE_VIDEO_GAME = "videogame"
        // other search types: boardgameartist, boardgamedesigner, boardgamepublisher

        const val HOTNESS_TYPE_BOARDGAME = "boardgame"

        const val FORUM_TYPE_REGION = "region"
        const val FORUM_TYPE_THING = "thing"
        const val FORUM_TYPE_PERSON = "person"
        const val FORUM_TYPE_COMPANY = "company"

        const val FORUM_REGION_BOARDGAME = 1
        // const val FORUM_REGION_RPG = 2
        // const val FORUM_REGION_VIDEOGAME = 3

        const val GEEK_LIST_SORT_HOT = "hot"
        const val GEEK_LIST_SORT_RECENT = "recent"
        const val GEEK_LIST_SORT_ACTIVE = "active"
    }
}
