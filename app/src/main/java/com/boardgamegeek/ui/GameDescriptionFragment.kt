package com.boardgamegeek.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.*
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlinx.android.synthetic.main.fragment_game_description.*
import kotlinx.android.synthetic.main.include_game_footer.*

class GameDescriptionFragment : Fragment() {
    private var gameId: Int = 0

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_description, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        gameId = arguments?.getInt(ARG_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")

        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setBggColors()

        gameIdView.text = gameId.toString()
        lastModifiedView.timestamp = 0L

        viewModel.game.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> showData(it.data)
            }
            progress.hide()
        })
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage?.text = message
            gameDescription.fadeOut()
            emptyMessage.fadeIn()
        }
    }

    private fun showData(game: GameEntity) {
        emptyMessage.fadeOut()

        gameDescription?.setTextMaybeHtml(game.description)
        gameDescription?.fadeIn()

        gameIdView?.text = game.id.toString()
        lastModifiedView?.timestamp = game.updated
    }

    companion object {
        private const val ARG_GAME_ID = "GAME_ID"

        @JvmStatic
        fun newInstance(gameId: Int): GameDescriptionFragment {
            val fragment = GameDescriptionFragment()
            val args = Bundle()
            args.putInt(ARG_GAME_ID, gameId)
            fragment.arguments = args
            return fragment
        }
    }
}
