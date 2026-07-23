package com.khiemnph.simpletracking.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.ItemSessionSummaryBinding

/** Diffs [HistorySummaryUiModel]s by [HistorySummaryUiModel.id], full equality for content changes. */
object HistorySummaryDiffCallback : DiffUtil.ItemCallback<HistorySummaryUiModel>() {

    override fun areItemsTheSame(oldItem: HistorySummaryUiModel, newItem: HistorySummaryUiModel): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: HistorySummaryUiModel, newItem: HistorySummaryUiModel): Boolean =
        oldItem == newItem
}

/**
 * Binds pre-formatted [HistorySummaryUiModel]s straight into row views - no formatting logic
 * here, that's all done upstream by [HistoryViewModel].
 */
class HistoryAdapter : ListAdapter<HistorySummaryUiModel, HistoryAdapter.ViewHolder>(HistorySummaryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemSessionSummaryBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // android:clipToOutline is only honored from the XML layout on API 31+; setting it
            // programmatically works back to API 21, which is what actually clips the loaded
            // thumbnail to bg_thumbnail_placeholder's rounded-corner outline on this app's
            // minSdk 29 devices.
            binding.historyItemThumbnail.clipToOutline = true
        }

        fun bind(item: HistorySummaryUiModel) {
            binding.historyItemRecordedAt.text = item.recordedAtLabel
            binding.historyItemDistance.text = item.distanceLabel
            binding.historyItemDuration.text = item.durationLabel
            binding.historyItemAverageSpeed.text = item.averageSpeedLabel
            binding.historyItemThumbnail.load(item.thumbnailPath) {
                // A null thumbnailPath (no live map to snapshot) resolves to Coil's "fallback"
                // path, not "error" - both point at the same neutral placeholder so either way
                // the row shows something sensible instead of a blank/broken image.
                placeholder(R.drawable.bg_thumbnail_placeholder)
                error(R.drawable.bg_thumbnail_placeholder)
                fallback(R.drawable.bg_thumbnail_placeholder)
            }
        }
    }
}
