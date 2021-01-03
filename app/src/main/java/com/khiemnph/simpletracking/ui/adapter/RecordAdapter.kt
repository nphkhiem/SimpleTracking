package com.khiemnph.simpletracking.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.utils.UIUtil
import com.khiemnph.simpletracking.utils.extension.toTextKilometre
import com.khiemnph.simpletracking.utils.extension.toTextSpeed
import kotlinx.android.synthetic.main.item_record.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class RecordAdapter: RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    private var data: ArrayList<ActivityRecord>? = null
    private val thumbMap: HashMap<String, Bitmap> = HashMap()
    private val sdf = SimpleDateFormat("dd/MM/yyyy")

    fun setData(data: ArrayList<ActivityRecord>) {
        if (this.data == null) {
            this.data = ArrayList(data)
        } else {
            this.data!!.clear()
            this.data!!.addAll(data)
        }
        anlAndMakeThumbnail()
    }

    fun anlAndMakeThumbnail() {
        if (!data.isNullOrEmpty()) {
            data!!.forEach { record ->
                val id = record.id
                val thumbByteArr = record.recordThumbByteArr
                val bitmap = BitmapFactory.decodeByteArray(thumbByteArr, 0, thumbByteArr?.size?:0)
                thumbMap[id!!] = bitmap
            }
            notifyDataSetChanged()
        }
    }

    inner class RecordViewHolder(val v: View): RecyclerView.ViewHolder(v) {
        fun bindData(record: ActivityRecord) {
            v.tvDate.text = sdf.format(Date(record.recordDate))
            Glide.with(v.context).load(thumbMap[record.id]).centerCrop().into(v.thumb)
            v.tvDistance.text = record.totalDistance.toTextKilometre()
            v.tvSpeed.text = record.speed.toTextSpeed()
            with(record.elapsedTime) {
                val second = ((this / 1000) % 60).toInt()
                val minute = ((this / 1000) / 60).toInt()
                val hour= ((this / 1000) / 60 / 60).toInt()
                v.tvElapsedTime.text = "${if (hour < 10) "0$hour" else hour}:" +
                        "${if (minute < 10) "0$minute" else minute}:" +
                        "${if (second < 10) "0$second" else second}"
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)).apply {
            this.itemView.thumb.updateLayoutParams<LinearLayout.LayoutParams> {
                width = UIUtil.screenWidth()
                height = width * 9 / 16
            }
        }
    }

    override fun getItemCount(): Int = data?.size ?: 0

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = data?.get(position)
        record?.let {
            holder.bindData(it)
        }
    }
}