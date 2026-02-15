package com.example.trennex.ui.product.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemReviewBinding
import com.example.trennex.ui.product.model.ReviewModel
import androidx.core.graphics.toColorInt

class ReviewAdapter(
 private val reviews:List<ReviewModel>
): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ReviewViewHolder,
        position: Int
    ) {
        val item = reviews[position]
        holder.binding.tvItemRating.text = String.format("%.1f ★", item.rating)
        holder.binding.tvReviewDate.text = item.time
        holder.binding.tvReviewContent.text = item.text

        val backgroundColor = when{
            item.rating >= 4.0 -> "#21AD60".toColorInt()
            item.rating >= 3.0 -> "#FBC02D".toColorInt()
            else -> "#D32F2F".toColorInt()
        }
        holder.binding.tvItemRating.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    override fun getItemCount(): Int {
        return reviews.size
    }

    inner  class ReviewViewHolder(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root)
}
