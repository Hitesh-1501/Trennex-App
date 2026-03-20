package com.example.trennex.ui.category.adapter

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.ui.category.model.ElectronicsCategoryModel

class ElectronicsAdapter(
    val items : List<ElectronicsCategoryModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object{
        const val TYPE_ELECTRONICS_BANNER = 0
        const val TYPE_ELECTRONICS_HEADER = 1
        const val TYPE_ELECTRONICS_ITEM = 2
    }


    override fun getItemViewType(position: Int): Int {
        return items[position].Type
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            TYPE_ELECTRONICS_BANNER -> {
                val view = inflater.inflate(R.layout.item_electronics_banner,parent,false)
                BannerVH(view)
            }
            TYPE_ELECTRONICS_HEADER -> {
                val view = inflater.inflate(R.layout.item_electronics_header,parent,false)
                HeaderVH(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_electronics_category,parent,false)
                CategoryVH(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        when(holder) {
            is HeaderVH -> {
                holder.tvHeader.text = item.title
            }
            is CategoryVH -> {
                holder.tvElectronics.text = item.title
                holder.ivElectronics.setImageResource(item.image!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class BannerVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvHeader = itemView.findViewById<TextView>(R.id.tvElectronicsHeader)
    }
    class CategoryVH(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvElectronics = itemView.findViewById<TextView>(R.id.tv_electronics)
        val ivElectronics = itemView.findViewById<ImageView>(R.id.iv_electronics)
    }
}