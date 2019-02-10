package com.boardgamegeek.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_links.*

class GameLinksFragment : Fragment() {
    @ColorInt
    private var iconColor = Color.TRANSPARENT

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_links, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        val gameName = arguments?.getString(KEY_GAME_NAME)

        if (gameId != BggContract.INVALID_ID) {
            geekbuddyAnalysisLink.setOnClickListener { context.linkToBgg("geekbuddy/analyze/thing", gameId) }
            bggLink.setOnClickListener { context.linkBgg(gameId) }
        }
        if (gameName != null && gameName.isNotBlank()) {
            bgPricesLink.setOnClickListener { context.linkBgPrices(gameName) }
            bgPricesUkLink.setOnClickListener { context.linkBgPricesUk(gameName) }
            amazonLink.setOnClickListener { context.linkAmazon(gameName, LINK_AMAZON_COM) }
            amazonUkLink.setOnClickListener { context.linkAmazon(gameName, LINK_AMAZON_UK) }
            amazonDeLink.setOnClickListener { context.linkAmazon(gameName, LINK_AMAZON_DE) }
            ebayLink.setOnClickListener { context.linkEbay(gameName) }
        }

        colorize(arguments?.getInt(KEY_ICON_COLOR, Color.TRANSPARENT) ?: Color.TRANSPARENT)
        viewModel.game.observe(this, Observer { game ->
            colorize(game?.data?.iconColor ?: Color.TRANSPARENT)
        })
    }

    private fun colorize(color: Int) {
        if (color != iconColor) {
            iconColor = color
            if (isAdded) {
                val icons = listOf(geekbuddyAnalysisLinkIcon, bggLinkIcon, bgPricesLinkIcon, amazonLinkIcon, ebayLinkIcon)
                if (iconColor == Color.TRANSPARENT) {
                    icons.forEach { it.clearColorFilter() }
                } else {
                    icons.forEach { it.setColorFilter(iconColor) }
                }
            }
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        @JvmStatic
        fun newInstance(gameId: Int, gameName: String, @ColorInt iconColor: Int): GameLinksFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)
            args.putInt(KEY_ICON_COLOR, iconColor)
            val fragment = GameLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
