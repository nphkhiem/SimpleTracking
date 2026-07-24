package com.khiemnph.simpletracking.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.FragmentRecordBinding

/**
 * Placeholder destination for an in-progress tracking session, reached from
 * [com.khiemnph.simpletracking.ui.MainActivity] either via user navigation or its cold-start
 * active-session recovery check. Phase 6 replaces this with the real recording UI; this exists
 * purely to be a valid, testable destination that proves the [args] Safe Args argument arrives.
 */
class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    val args: RecordFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recordPlaceholderText.text = getString(R.string.record_placeholder_text, args.sessionId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
