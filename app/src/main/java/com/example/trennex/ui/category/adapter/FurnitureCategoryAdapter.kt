package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.ui.category.model.FurnitureCategoryModel

class FurnitureCategoryAdapter(
    private val items : List<FurnitureCategoryModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object{
        const val TYPE_FURNITURE_BANNER = 0
        const val TYPE_FURNITURE_HEADER = 1
        const val TYPE_FURNITURE_ITEM = 2
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].Type
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when(viewType){
            TYPE_FURNITURE_BANNER -> {
                val view = inflater.inflate(R.layout.item_beauty_banner,parent,false)
                return BannerVH(view)
            }
            TYPE_FURNITURE_HEADER -> {
                val view = inflater.inflate(R.layout.item_beauty_header,parent,false)
                return HeaderVH(view)
            }
            else  ->{
                val view = inflater.inflate(R.layout.item_beauty_category,parent,false)
                return ItemVH(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        when(holder){
            is HeaderVH ->{
                holder.tvHeader.text = item.title
            }
            is ItemVH ->{
                holder.tvName.text = item.title
                holder.ivImage.setImageResource(item.image!!)
            }
        }
    }

    override fun getItemCount(): Int {
       return items.size
    }

    class BannerVH(view: View): RecyclerView.ViewHolder(view)
    class HeaderVH(view: View): RecyclerView.ViewHolder(view){
        val tvHeader : TextView = view.findViewById(R.id.tvFurnitureHeader)
    }
    class ItemVH(view: View):RecyclerView.ViewHolder(view){
        val tvName:TextView = view.findViewById(R.id.tv_furniture)
        val ivImage: ImageView = view.findViewById(R.id.iv_furniture)
    }
}