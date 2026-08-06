package com.musicdownloader.musicfreeapp825v2.ui.fragments.settings

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import com.applogevent.logeventlib.LogEventLibs
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.ActivityFeedbackBinding
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgeProperly
import com.musicdownloader.musicfreeapp825v2.logic.ui.BaseActivity
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.Keys
import com.musicdownloader.musicfreeapp825v2.ui.components.BottomSheetFeedbackSuccess

/**
 * Feedback & Help screen, ported from MSDownloader07's `FeedbackActivity`: pick issue chips + type
 * details (Submit appears once >= 6 chars). Submit logs the feedback (Firebase + LogEventLibs) and
 * shows a thank-you bottom sheet, then finishes.
 */
class FeedbackActivity : BaseActivity() {

    private val binding by lazy { ActivityFeedbackBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        binding.imgFeedback.setImageResource(
            if (isDark) R.drawable.ic_feedback_dark else R.drawable.ic_feedback_light
        )

        binding.btnBack.setOnClickListener { finish() }

        // Submit only becomes available once the details field has at least 6 characters.
        binding.edtFeedback.doOnTextChanged { text, _, _, _ ->
            binding.btnSubmit.visibility = if ((text?.length ?: 0) >= 6) View.VISIBLE else View.GONE
        }

        binding.btnSubmit.setOnClickListener {
            val details = binding.edtFeedback.text?.toString()?.trim().orEmpty()
            if (details.isBlank()) {
                Toast.makeText(
                    this, R.string.you_have_not_filled_in_your_feedback_yet, Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            FirebaseEventUtils.getInstances().logEvent(this, Keys.USER_SEND_FEEDBACK)
            val content = checkedIssues().joinToString(", ") + " ," + details
            LogEventLibs.logFeedBack(content)
            BottomSheetFeedbackSuccess { finish() }.show(supportFragmentManager, "FeedbackSuccess")
        }
    }

    private fun checkedIssues(): List<String> =
        listOf(
            binding.issue1, binding.issue2, binding.issue3,
            binding.issue4, binding.issue5, binding.issue6
        ).filter { it.isChecked }.map { it.text.toString() }
}
