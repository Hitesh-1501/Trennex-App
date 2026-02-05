package com.example.trennex.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemBannerBinding
import com.example.trennex.ui.home.model.BannerModel

class BannerAdapter(
   private val banners: List<BannerModel>
): RecyclerView.Adapter<BannerAdapter.BannerViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BannerViewHolder,
        position: Int
    ) {
        val item = banners[position]
        holder.binding.banner.setImageResource(item.banner)
    }

    override fun getItemCount(): Int {
        return banners.size
    }

    inner class BannerViewHolder(val binding: ItemBannerBinding): RecyclerView.ViewHolder(binding.root)
}