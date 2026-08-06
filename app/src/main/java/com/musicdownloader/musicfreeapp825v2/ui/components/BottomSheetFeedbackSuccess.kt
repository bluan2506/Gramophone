package com.musicdownloader.musicfreeapp825v2.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicdownloader.musicfreeapp825v2.databinding.BottomSheetFeedbackSuccessBinding

/**
 * "Thank you for your feedback" confirmation sheet shown after submitting feedback.
 * Ported from the MSDownloader07 `BottomSheetFeedbackSuccess`; [dismissClick] runs when OK is tapped
 * (the Feedback screen uses it to finish itself).
 */
class BottomSheetFeedbackSuccess(private val dismissClick: () -> Unit) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFeedbackSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetFeedbackSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOK.setOnClickListener {
            dismiss()
            dismissClick()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
