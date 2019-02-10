package com.boardgamegeek.ui

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.*
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.ui.dialog.GameRanksFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow
import com.boardgamegeek.ui.widget.SafeViewTarget
import com.boardgamegeek.util.HelpUtils
import com.boardgamegeek.util.ShowcaseViewWizard
import com.github.amlcurran.showcaseview.targets.Target
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.include_game_ages.*
import kotlinx.android.synthetic.main.include_game_footer.*
import kotlinx.android.synthetic.main.include_game_language_dependence.*
import kotlinx.android.synthetic.main.include_game_player_range.*
import kotlinx.android.synthetic.main.include_game_playing_time.*
import kotlinx.android.synthetic.main.include_game_ranks.*
import kotlinx.android.synthetic.main.include_game_ratings.*
import kotlinx.android.synthetic.main.include_game_weight.*
import kotlinx.android.synthetic.main.include_game_year_published.*

class GameFragment : Fragment() {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var showcaseViewWizard: ShowcaseViewWizard? = null
    @Suppress("DEPRECATION")
    private val rankSeparator = "  " + Html.fromHtml("&#9679;") + "  "

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")
        gameName = arguments?.getString(KEY_GAME_NAME) ?: ""


        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        gameIdView?.text = gameId.toString()
        lastModifiedView?.timestamp = 0

        viewModel.game.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            progress.hide()
        })

        viewModel.ranks.observe(this, Observer { gameRanks -> onRankQueryComplete(gameRanks) })

        viewModel.languagePoll.observe(this, Observer { gamePollEntity -> onLanguagePollQueryComplete(gamePollEntity) })

        viewModel.agePoll.observe(this, Observer { gameSuggestedAgePollEntity -> onAgePollQueryComplete(gameSuggestedAgePollEntity) })

        viewModel.playerPoll.observe(this, Observer { gamePlayerPollEntities -> onPlayerCountQueryComplete(gamePlayerPollEntities) })

        viewModel.designers.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_designers) })

        viewModel.artists.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_artists) })

        viewModel.publishers.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_publishers) })

        viewModel.categories.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_categories) })

        viewModel.mechanics.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_mechanics) })

        viewModel.expansions.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_expansions) })

        viewModel.baseGames.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_base_games) })

        showcaseViewWizard = setUpShowcaseViewWizard()
        showcaseViewWizard?.maybeShowHelp()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.help, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_help) {
            showcaseViewWizard?.showHelp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpShowcaseViewWizard(): ShowcaseViewWizard {
        val wizard = ShowcaseViewWizard(activity, HelpUtils.HELP_GAME_KEY, HELP_VERSION)
        wizard.addTarget(R.string.help_game_menu, Target.NONE)
        wizard.addTarget(R.string.help_game_poll, SafeViewTarget(R.id.playerCountContainer, requireActivity()))
        wizard.addTarget(0, SafeViewTarget(R.id.playerAgeContainer, requireActivity()))
        return wizard
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage?.text = message
            dataContainer?.fadeOut()
            emptyMessage?.fadeIn()
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        listOf(ranksIcon, ratingIcon, yearIcon, playTimeIcon, playerCountIcon, playerAgeIcon, weightIcon, languageIcon)
                .forEach { it?.setOrClearColorFilter(iconColor) }
        listOf(game_info_designers, game_info_artists, game_info_publishers, game_info_categories, game_info_mechanics, game_info_expansions, game_info_base_games)
                .forEach { it?.colorize(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        gameName = game.name

        rankView?.text = game.overallRank.asRank(requireContext(), game.subtype)
        rankContainer?.setOnClickListener { GameRanksFragment.launch(this) }

        ratingView?.text = game.rating.asRating(context)
        ratingView.setTextViewBackground(game.rating.toColor(ratingColors))
        val numberOfRatings = context?.getQuantityText(R.plurals.ratings_suffix, game.numberOfRatings, game.numberOfRatings)
                ?: ""
        val numberOfComments = context?.getQuantityText(R.plurals.comments_suffix, game.numberOfComments, game.numberOfComments)
                ?: ""
        ratingVotesView?.text = listOf(numberOfRatings, " & ", numberOfComments).concat()
        ratingContainer?.setOrClearOnClickListener(game.numberOfRatings > 0 || game.numberOfComments > 0) {
            CommentsActivity.startRating(context, Games.buildGameUri(gameId), gameName)
        }

        yearView?.text = game.yearPublished.asYear(context)

        playTimeView?.text = context?.getQuantityText(R.plurals.mins_suffix, game.maxPlayingTime, (game.minPlayingTime to game.maxPlayingTime).asRange()) ?: ""

        playerCountView?.text = context?.getQuantityText(R.plurals.player_range_suffix, game.maxPlayers, (game.minPlayers to game.maxPlayers).asRange()) ?: ""

        playerAgeView?.text = game.minimumAge.asAge(context)

        weightView?.text = game.averageWeight.toDescription(requireContext(), R.array.game_weight, R.string.unknown_weight)
        weightView?.setTextViewBackground(game.averageWeight.toColor(fiveStageColors))
        weightScoreView?.setTextOrHide(game.averageWeight.asScore(context))
        weightVotesView?.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, game.numberOfUsersWeighting, game.numberOfUsersWeighting))

        gameIdView?.text = game.id.toString()
        lastModifiedView?.timestamp = game.updated

        emptyMessage?.fadeOut()
        dataContainer?.fadeIn()
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>?, view: GameDetailRow?) {
        view?.bindData(gameId, gameName, list)
    }

    private fun onRankQueryComplete(gameRanks: List<GameRankEntity>?) {
        val descriptions = gameRanks?.filter { it.isFamilyType }?.map { it.value.asRank(requireContext(), it.name, it.type) }
                ?: emptyList()
        subtypeView.setTextOrHide(descriptions.joinTo(rankSeparator))
    }

    private fun onLanguagePollQueryComplete(entity: GamePollEntity?) {
        val score = entity?.calculateScore() ?: 0.0
        languageView?.text = score.toDescription(requireContext(), R.array.language_poll, R.string.unknown_language)
        languageView?.setTextViewBackground(score.toColor(fiveStageColors))
        languageScoreView?.setTextOrHide(score.asScore(context))
        val totalVotes = entity?.totalVotes ?: 0
        languageVotesView?.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, totalVotes, totalVotes))
        languageContainer?.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            PollFragment.launchLanguageDependence(this)
        }
    }

    private fun onAgePollQueryComplete(entity: GamePollEntity?) {
        val message = if (entity?.modalValue.isNullOrBlank()) ""
        else context?.getText(R.string.age_community, entity?.modalValue ?: "") ?: ""
        playerAgePollView?.setTextOrHide(message)
        playerAgeContainer?.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            PollFragment.launchSuggestedPlayerAge(this)
        }
    }

    private fun onPlayerCountQueryComplete(entity: GamePlayerPollEntity?) {
        val bestCounts = entity?.bestCounts ?: emptyList()
        val goodCounts = entity?.recommendedCounts ?: emptyList()

        val best = context?.getText(R.string.best_prefix, bestCounts.asRange(max = maxPlayerCount)) ?: ""
        val good = context?.getText(R.string.recommended_prefix, goodCounts.asRange(max = maxPlayerCount)) ?: ""
        val communityText = when {
            bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts -> TextUtils.concat(best, " & ", good)
            bestCounts.isNotEmpty() -> best
            goodCounts.isNotEmpty() -> good
            else -> ""
        }
        playerCountCommunityView?.setTextOrHide(communityText)
        playerCountContainer?.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            SuggestedPlayerCountPollFragment.launch(this)
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        private const val HELP_VERSION = 2

        fun newInstance(gameId: Int, gameName: String): GameFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)
            val fragment = GameFragment()
            fragment.arguments = args
            return fragment
        }
    }
}