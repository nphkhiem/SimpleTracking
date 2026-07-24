package com.khiemnph.simpletracking.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.khiemnph.simpletracking.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Start destination for [com.khiemnph.simpletracking.ui.MainActivity]'s Navigation graph: the
 * session-history feed list, plus a Record button that starts a brand-new session (no sessionId
 * yet - [com.khiemnph.simpletracking.ui.record.RecordFragment] fills one in once
 * `StartSessionUseCase` runs in a later phase).
 */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = HistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyRecyclerView.adapter = adapter

        binding.historyRecordButton.setOnClickListener {
            findNavController().navigate(
                HistoryFragmentDirections.actionHistoryFragmentToRecordFragment(sessionId = null),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { summaries ->
                    adapter.submitList(summaries)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.historyRecyclerView.adapter = null
        _binding = null
    }
}
