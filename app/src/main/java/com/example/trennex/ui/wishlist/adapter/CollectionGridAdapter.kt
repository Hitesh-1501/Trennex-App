package com.example.trennex.ui.wishlist.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemCollectionAddBinding
import com.example.trennex.databinding.ItemCollectionBinding
import com.example.trennex.ui.wishlist.model.CollectionModel

class CollectionGridAdapter(
    private val onCreateCollectionClicked: () -> Unit,
    private val onCollectionMenuClicked: (anchor: View,item: CollectionModel) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var items: List<CollectionModel> = emptyList()

    companion object{
        private const val TYPE_ADD = 0
        private const val TYPE_COLLECTION = 1
    }

    inner class AddVH(val binding: ItemCollectionAddBinding): RecyclerView.ViewHolder(binding.root)
    inner class CollectionVH(val binding: ItemCollectionBinding): RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int = if(position == 0) TYPE_ADD else TYPE_COLLECTION

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if(viewType == TYPE_ADD){
            val binding = ItemCollectionAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AddVH(binding)
        }else{
            val binding = ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            CollectionVH(binding)
        }
    }
    override fun getItemCount(): Int  = items.size + 1

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (position == 0) {
            (holder as? AddVH)?.binding?.root?.setOnClickListener { onCreateCollectionClicked() }
            return
        }
        val dataIndex = position - 1
        if(dataIndex !in items.indices) return
        val item = items[dataIndex]
        val collectionHolder = holder as CollectionVH
        collectionHolder.binding.tvCollectionName.text = item.name
        val itemCount = item.items.size
        collectionHolder.binding.tvItemCount.text = if (itemCount == 1) "1 item" else "$itemCount items"

        val images = item.items.map { it.imageUrl }
        loadImage(collectionHolder.binding.ivMain,images.getOrNull(0))
        renderPreviewImages(collectionHolder.binding,images)
        collectionHolder.binding.ivMenu.setOnClickListener {
            onCollectionMenuClicked(it, item)
        }
    }

    private fun renderPreviewImages(binding: ItemCollectionBinding, images: List<String>){
        val imageCount = images.size
        val mainParams = binding.ivMain.layoutParams as LinearLayout.LayoutParams
        val topParams = binding.ivTop.layoutParams as LinearLayout.LayoutParams

        when{
            imageCount <= 1 -> {
                binding.collectionRightColumn.visibility = View.GONE
                mainParams.weight = 2f
            }
            imageCount == 2 -> {
                binding.collectionRightColumn.visibility = View.VISIBLE
                binding.ivTop.visibility = View.VISIBLE
                binding.ivBottom.visibility = View.GONE
                topParams.weight = 2f
                loadImage(binding.ivTop,images.getOrNull(1))
                mainParams.weight = 1f
            }
            else -> {
                binding.collectionRightColumn.visibility = View.VISIBLE
                binding.ivTop.visibility = View.VISIBLE
                binding.ivBottom.visibility = View.VISIBLE
                topParams.weight = 1f
                loadImage(binding.ivTop, images.getOrNull(1))
                loadImage(binding.ivBottom, images.getOrNull(2))
                mainParams.weight = 1f
            }
        }
        binding.ivMain.layoutParams = mainParams
        binding.ivTop.layoutParams = topParams
    }

    private fun loadImage(view: ImageView,url: String?){
        Glide.with(view)
            .load(url)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(view)
    }



    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<CollectionModel>) {
        items = newItems
        notifyDataSetChanged()
    }

}