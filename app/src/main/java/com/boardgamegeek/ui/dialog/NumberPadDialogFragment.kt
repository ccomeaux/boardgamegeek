package com.boardgamegeek.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.isColorDark
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_number_pad.*
import org.jetbrains.anko.childrenRecursiveSequence

class NumberPadDialogFragment : DialogFragment() {
    private var listener: Listener? = null
    private var minValue = DEFAULT_MIN_VALUE
    private var maxValue = DEFAULT_MAX_VALUE
    private var maxMantissa = DEFAULT_MAX_MANTISSA

    interface Listener {
        fun onNumberPadDone(output: Double, requestCode: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }

    override fun onResume() {
        super.onResume()
        val window = dialog.window
        if (window != null) {
            val dm = resources.displayMetrics
            val width = Math.min(
                    requireActivity().resources.getDimensionPixelSize(R.dimen.dialog_width),
                    dm.widthPixels * 3 / 4)
            val height = window.attributes.height
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_number_pad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        minValue = arguments?.getDouble(KEY_MIN_VALUE) ?: DEFAULT_MIN_VALUE
        maxValue = arguments?.getDouble(KEY_MAX_VALUE) ?: DEFAULT_MAX_VALUE
        maxMantissa = arguments?.getInt(KEY_MAX_MANTISSA) ?: DEFAULT_MAX_MANTISSA

        plusMinusView.visibility = if (minValue >= 0.0) View.GONE else View.VISIBLE

        val titleResId = arguments?.getInt(KEY_TITLE) ?: 0
        if (titleResId != 0) titleView.setText(titleResId)
        subtitleView.setTextOrHide(arguments?.getString(KEY_SUBTITLE))

        if (arguments?.containsKey(KEY_INITIAL_VALUE) == true) {
            outputView.text = arguments?.getString(KEY_INITIAL_VALUE)
            enableDelete()
        }

        if (arguments?.containsKey(KEY_COLOR) == true) {
            val color = arguments?.getInt(KEY_COLOR) ?: Color.TRANSPARENT
            headerView.setBackgroundColor(color)
            if (color != Color.TRANSPARENT && color.isColorDark()) {
                titleView.setTextColor(Color.WHITE)
                subtitleView.setTextColor(Color.WHITE)
            } else {
                titleView.setTextColor(Color.BLACK)
                subtitleView.setTextColor(Color.BLACK)
            }
        }

        deleteView.setOnClickListener {
            val text = outputView.text
            if (text.isNotEmpty()) {
                val output = text.subSequence(0, text.length - 1).toString()
                maybeUpdateOutput(output, view)
            }
        }
        deleteView.setOnLongClickListener {
            outputView.text = ""
            enableDelete()
            true
        }

        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: DEFAULT_REQUEST_CODE
        doneView.setOnClickListener {
            listener?.onNumberPadDone(parseDouble(outputView.text.toString()), requestCode)
            dismiss()
        }

        plusMinusView.setOnClickListener {
            val output = outputView.text.toString()
            val signedOutput = if (output.isNotEmpty() && output[0] == '-') {
                output.substring(1)
            } else {
                "-$output"
            }
            maybeUpdateOutput(signedOutput, it)
        }

        numberPadView.childrenRecursiveSequence().filterIsInstance<TextView>().forEach {
            it.setOnClickListener { textView ->
                val output = outputView.text.toString() + (textView as TextView).text
                maybeUpdateOutput(output, it)
            }
        }
    }

    private fun maybeUpdateOutput(output: String, view: View) {
        if (isWithinLength(output) && isWithinRange(output)) {
            maybeBuzz(view)
            outputView.text = output
            enableDelete()
        }
    }

    private fun maybeBuzz(v: View) {
        if (PreferencesUtils.getHapticFeedback(context)) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun isWithinLength(text: String): Boolean {
        return text.isEmpty() || text.length <= DEFAULT_MAX_MANTISSA && getMantissaLength(text) <= maxMantissa
    }

    private fun getMantissaLength(text: String): Int {
        if (!text.contains(".")) {
            return 0
        }
        val parts = text.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (parts.size > 1) {
            parts[1].length
        } else 0
    }

    private fun isWithinRange(text: String): Boolean {
        if (text.isEmpty() || "." == text || "-." == text) {
            return true
        }
        if (hasTwoDecimalPoints(text)) {
            return false
        }
        val value = parseDouble(text)
        return value in minValue..maxValue
    }

    private fun hasTwoDecimalPoints(text: String?): Boolean {
        if (text == null) return false
        val decimalIndex = text.indexOf('.')
        return decimalIndex >= 0 && text.indexOf('.', decimalIndex + 1) >= 0
    }

    private fun parseDouble(text: String): Double {
        return when {
            text.isEmpty() || "." == text || "-" == text || "-." == text -> 0.0
            text.endsWith(".") -> "${text}0".toDouble()
            text.startsWith(".") -> "0$text".toDouble()
            text.startsWith("-.") -> ("-0" + text.substring(1)).toDouble()
            else -> text.toDouble()
        }
    }

    private fun enableDelete() {
        deleteView.isEnabled = outputView.length() > 0
    }

    companion object {
        private const val KEY_TITLE = "TITLE"
        private const val KEY_SUBTITLE = "SUBTITLE"
        private const val KEY_INITIAL_VALUE = "INITIAL_VALUE"
        private const val KEY_COLOR = "COLOR"
        private const val KEY_MIN_VALUE = "MIN_VALUE"
        private const val KEY_MAX_VALUE = "MAX_VALUE"
        private const val KEY_MAX_MANTISSA = "MAX_MANTISSA"
        private const val KEY_REQUEST_CODE = "REQUEST_CODE"
        private val DEFAULT_MIN_VALUE = -Double.MAX_VALUE
        private val DEFAULT_MAX_VALUE = Double.MAX_VALUE
        private const val DEFAULT_MAX_MANTISSA = 10
        private const val DEFAULT_REQUEST_CODE = 0

        @JvmStatic
        @JvmOverloads
        fun newInstance(
                requestCode: Int,
                @StringRes titleResId: Int,
                initialValue: String,
                colorDescription: String? = null,
                subtitle: String? = null,
                minValue: Double = DEFAULT_MIN_VALUE,
                maxValue: Double = DEFAULT_MAX_VALUE,
                maxMantissa: Int = DEFAULT_MAX_MANTISSA
        ): NumberPadDialogFragment {
            return NumberPadDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_TITLE, titleResId)
                    if (!subtitle.isNullOrBlank()) {
                        putString(KEY_SUBTITLE, subtitle)
                    }
                    if (initialValue.toDoubleOrNull() != null) {
                        putString(KEY_INITIAL_VALUE, initialValue)
                    }
                    val color = colorDescription.asColorRgb()
                    if (color != Color.TRANSPARENT) {
                        putInt(KEY_COLOR, color)
                    }
                    putDouble(KEY_MIN_VALUE, minValue)
                    putDouble(KEY_MAX_VALUE, maxValue)
                    putInt(KEY_MAX_MANTISSA, maxMantissa)
                    putInt(KEY_REQUEST_CODE, requestCode)
                }
            }
        }

        @JvmStatic
        @JvmOverloads
        fun newInstanceForRating(requestCode: Int,
                                 @StringRes titleResId: Int,
                                 initialValue: String,
                                 colorDescription: String? = null,
                                 subtitle: String? = null
        ): NumberPadDialogFragment {
            return newInstance(requestCode, titleResId, initialValue, colorDescription, subtitle, 1.0, 10.0, 6)
        }
    }
}
