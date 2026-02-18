package com.example.trennex.ui.product.adapter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

class FullImageAdapter(
    private val images: List<Int>,
    private val startPosition : Int
): RecyclerView.Adapter<FullImageAdapter.ImageVH>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageVH {
        val image = ImageView(parent.context).apply {
         layoutParams = ViewGroup.LayoutParams(
             ViewGroup.LayoutParams.MATCH_PARENT,
             ViewGroup.LayoutParams.MATCH_PARENT
             )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        return ImageVH(image)
    }

    override fun onBindViewHolder(
        holder: ImageVH,
        position: Int
    ) {
        holder.img.setImageResource(images[position])
        ViewCompat.setTransitionName(holder.img,null)
        if(position == startPosition){
            ViewCompat.setTransitionName(holder.img,"product_image")
        }
    }

    override fun getItemCount(): Int {
      return images.size
    }

    inner class ImageVH(val img : ImageView): RecyclerView.ViewHolder(img)




}