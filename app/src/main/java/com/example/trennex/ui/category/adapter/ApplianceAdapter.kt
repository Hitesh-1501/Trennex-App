package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.ui.category.model.ApplianceCategoryModel

class ApplianceAdapter(
    private val list : List<ApplianceCategoryModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object{
        const val TYPE_BANNER = 0
        const val TYPE_HEADER = 1
        const val TYPE_ITEM = 2
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].Type
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
       val inflater = LayoutInflater.from(parent.context)
       when(viewType){
           TYPE_BANNER -> {
               val view = inflater.inflate(R.layout.item_appliance_banner,parent,false)
               return BannerVH(view)
           }
           TYPE_HEADER ->{
               val view = inflater.inflate(R.layout.item_appliance_header,parent,false)
               return HeaderVH(view)
           }
           else -> {
               val view = inflater.inflate(R.layout.item_appliance_item,parent,false)
               return ItemVH(view)
           }
       }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = list[position]
        when(holder){
            is HeaderVH -> {
                holder.tvHeader.text = item.title
            }
            is ItemVH -> {
                holder.tvName.text = item.title
                holder.ivImage.setImageResource(item.image!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class BannerVH(view: View): RecyclerView.ViewHolder(view)
    class HeaderVH(view: View): RecyclerView.ViewHolder(view){
        val tvHeader : TextView = view.findViewById(R.id.tvHeader)

    }
    class ItemVH(view: View):RecyclerView.ViewHolder(view){
        val tvName:TextView = view.findViewById(R.id.tv_appliance)
        val ivImage: ImageView = view.findViewById(R.id.iv_appliance)

    }

}