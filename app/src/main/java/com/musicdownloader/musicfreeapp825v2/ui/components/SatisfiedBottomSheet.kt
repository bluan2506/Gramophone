package com.musicdownloader.musicfreeapp825v2.ui.components

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicdownloader.musicfreeapp825v2.databinding.BottomSheetSatisfiedBinding
import com.musicdownloader.musicfreeapp825v2.logic.padOutOfSystemBars
import com.musicdownloader.musicfreeapp825v2.ui.fragments.settings.FeedbackActivity

/**
 * First-time rate prompt ("Are you satisfied?"), ported from MSDownloader07's `SatisfiedBottomSheet`.
 * "Good" leads to the star-rating sheet; "Not really" opens the Feedback screen.
 */
class SatisfiedBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SatisfiedBottomSheet"

        @JvmStatic
        var isShowingDialog = false
            private set
    }

    private var _binding: BottomSheetSatisfiedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetSatisfiedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        view.padOutOfSystemBars()

        binding.tvGood.setOnClickListener {
            RateAppBottomSheet.newInstance(false)
                .show(parentFragmentManager, RateAppBottomSheet.TAG)
            dismiss()
        }

        binding.tvNotReally.setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
            dismiss()
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
