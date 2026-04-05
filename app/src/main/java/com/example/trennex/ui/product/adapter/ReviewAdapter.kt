package com.example.trennex.ui.product.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.util.TimeZone
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemReviewBinding
import com.example.trennex.ui.product.model.ReviewModel
import androidx.core.graphics.toColorInt
import com.example.trennex.data.model.ReviewResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter(
 private var reviews:List<ReviewModel>
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
        holder.binding.tvReviewDate.text = getTimeAgo(item.time)
        holder.binding.tvReviewContent.text = item.comment
        holder.binding.tvUserName.text = item.reviewerName

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

    fun getTimeAgo(dateString : String): String{
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val reviewDate = format.parse(dateString) ?: return ""
        val diff = Date().time - reviewDate.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        return when {
            months > 0 -> "$months months ago"
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes min ago"
            else -> "Just now"
        }

    }


    fun updateList(newList: List<ReviewModel>) {
        reviews = newList
        notifyDataSetChanged()
    }

    inner  class ReviewViewHolder(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root)
}
