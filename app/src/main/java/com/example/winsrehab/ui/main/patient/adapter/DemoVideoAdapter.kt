package com.example.winsrehab.ui.main.patient.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.winsrehab.data.entity.DemoVideo
import com.example.winsrehab.databinding.ItemVideoBinding

class DemoVideoAdapter(
    private var data: List<DemoVideo>,
    private val onItemClick: (DemoVideo) -> Unit
) : RecyclerView.Adapter<DemoVideoAdapter.VH>() {

    inner class VH(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        //有空补课
        fun bind(video: DemoVideo) {
            binding.tvTitle.text = video.title
            binding.tvDescription.text = video.description
            binding.root.setOnClickListener { onItemClick(video) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])
    override fun getItemCount() = data.size

    fun submitList(newList: List<DemoVideo>) {
        data = newList
        notifyDataSetChanged()
    }
}