package com.example.trennex.ui.search.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.databinding.ItemSearchResultGridBinding
import com.example.trennex.databinding.ItemSearchResultListBinding
import com.example.trennex.utils.CurrencyFormator
import com.example.trennex.viewmodel.product.ProductViewModel
import kotlin.math.roundToInt

class SearchResultAdapter(
    private val onProductClick: (ProductResponse) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val items = mutableListOf<ProductResponse>()
    private var isGrid = false

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(newItems: List<ProductResponse>, grid: Boolean){
        items.clear()
        items.addAll(newItems)
        isGrid = grid
        notifyDataSetChanged()
    }


    override fun getItemViewType(position: Int): Int = if(isGrid) 1 else 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
       return if(viewType == 1){
           GridVH(ItemSearchResultGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))
       }else{
           ListVH(ItemSearchResultListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
       }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
       val item = items[position]
        if(holder is GridVH)  holder.bind(item) else (holder as ListVH).bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class GridVH(private val binding: ItemSearchResultGridBinding): RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(item: ProductResponse){
            val mrp = item.catalogVariants.firstOrNull()?.mrp ?: if (item.discountPercentage > 0){
                item.price / (1 - (item.discountPercentage / 100.0))
            }else{
                item.price
            }

            val mrpText = CurrencyFormator.formatInr(mrp)
            val spannable = SpannableString(mrpText)
            spannable.setSpan(StrikethroughSpan(), 0, mrpText.length, 0)
            val discountPercent = if (mrp > item.price) ((mrp - item.price) * 100 / mrp).roundToInt() else 0
            binding.discountRow.visibility = if (discountPercent > 0) View.VISIBLE else View.GONE
            binding.discount.text = "$discountPercent% OFF"
            binding.title.text = item.title
            binding.description.text = item.description
            binding.mrpPrice.paintFlags = binding.mrpPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.mrpPrice.text = spannable
            binding.price.text = CurrencyFormator.formatInr(item.price)
            Glide.with(binding.image).load(item.thumbnail).into(binding.image)
            binding.root.setOnClickListener { onProductClick(item) }
        }
    }
    inner class ListVH(private val binding: ItemSearchResultListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductResponse) {
            val mrp = item.catalogVariants.firstOrNull()?.mrp ?: if (item.discountPercentage > 0){
                item.price / (1 - (item.discountPercentage / 100.0))
            }else{
                item.price
            }

            val mrpText = CurrencyFormator.formatInr(mrp)
            val spannable = SpannableString(mrpText)
            spannable.setSpan(StrikethroughSpan(), 0, mrpText.length, 0)
            binding.title.text = item.title
            binding.description.text = item.description
            binding.mrpPrice.paintFlags = binding.mrpPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.mrpPrice.text = spannable
            val discountPercent = if (mrp > item.price) ((mrp - item.price) * 100 / mrp).roundToInt() else 0
            binding.discountRow.visibility = if (discountPercent > 0) View.VISIBLE else View.GONE
            binding.discount.text = "$discountPercent% OFF"
            binding.price.text = CurrencyFormator.formatInr(item.price)
            Glide.with(binding.image).load(item.thumbnail).into(binding.image)
            binding.root.setOnClickListener { onProductClick(item) }

        }
    }
}
