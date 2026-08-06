package com.musicdownloader.musicfreeapp825v2.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.play.core.review.ReviewManagerFactory
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.BottomSheetRateAppBinding
import com.musicdownloader.musicfreeapp825v2.logic.utils.RateAppUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.ui.fragments.settings.FeedbackActivity

/**
 * Star-rating prompt, ported from MSDownloader07's `RateAppBottomSheet`.
 * 5 stars launches the Google Play In-App Review flow; 1-4 stars opens the Feedback screen.
 */
class RateAppBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "RateAppBottomSheet"

        private const val CAN_INTERACT_RATE_APP_COUNT = "CAN_INTERACT_RATE_APP_COUNT"

        @JvmStatic
        var isShowingDialog = false
            private set

        fun newInstance(canInteractRateAppCount: Boolean): RateAppBottomSheet {
            return RateAppBottomSheet().apply {
                arguments = bundleOf(CAN_INTERACT_RATE_APP_COUNT to canInteractRateAppCount)
            }
        }
    }

    private var _binding: BottomSheetRateAppBinding? = null
    private val binding get() = _binding!!

    private val canInteractRateAppCount by lazy {
        arguments?.getBoolean(CAN_INTERACT_RATE_APP_COUNT) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetRateAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleListener()
    }

    private fun handleListener() {
        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, _ ->
            if (rating > 0f) {
                binding.tvRate.setBackgroundResource(R.drawable.bg_button_primary)
                binding.tvRate.setTextColor(
                    MaterialColors.getColor(binding.tvRate, com.google.android.material.R.attr.colorOnPrimary)
                )
            } else {
                binding.tvRate.setBackgroundResource(R.drawable.bg_button_secondary)
                binding.tvRate.setTextColor(
                    MaterialColors.getColor(binding.tvRate, com.google.android.material.R.attr.colorOnSecondaryContainer)
                )
            }
            when {
                rating < 1f -> ratingBar.rating = 1f
                rating == 1f -> {
                    binding.tvTitle.setText(R.string.rate_app_title_1)
                    binding.imgStar.setImageResource(R.drawable.ic_star_1)
                }
                rating == 2f -> {
                    binding.tvTitle.setText(R.string.rate_app_title_1)
                    binding.imgStar.setImageResource(R.drawable.ic_star_2)
                }
                rating == 3f -> {
                    binding.tvTitle.setText(R.string.rate_app_title_1)
                    binding.imgStar.setImageResource(R.drawable.ic_star_3)
                }
                rating == 4f -> {
                    binding.tvTitle.setText(R.string.rate_app_title_4)
                    binding.imgStar.setImageResource(R.drawable.ic_star_4)
                }
                rating == 5f -> showRateApp()
            }
        }

        binding.tvRate.setOnClickListener {
            val rating = binding.ratingBar.rating
            if (rating == 5f) {
                showRateApp()
            } else if (rating >= 1f) {
                showFeedback()
            }
        }
    }

    private fun showRateApp() {
        if (canInteractRateAppCount) {
            RateAppUtils.interactRateApp(requireContext())
        }
        launchInAppReview()
        dismiss()
    }

    private fun showFeedback() {
        if (canInteractRateAppCount) {
            RateAppUtils.interactRateApp(requireContext())
        }
        startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        dismiss()
    }

    // Mirrors HomeFragment.rateApp(): request + launch the In-App Review flow, then open the store.
    private fun launchInAppReview() {
        val activity = activity ?: return
        FirebaseEventUtils.getInstances().logEventUserClickRateApp(activity)
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result).addOnCompleteListener { openPlayStore() }
            } else {
                openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        val activity = activity ?: return
        val pkg = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$pkg".toUri()
                )
            )
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.isStateSaved) return
        runCatching {
            super.show(manager, tag)
            isShowingDialog = true
        }
    }

    override fun dismiss() {
        isShowingDialog = false
        super.dismiss()
    }

    override fun onDestroyView() {
        isShowingDialog = false
        _binding = null
        super.onDestroyView()
    }
}