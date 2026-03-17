package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentFashionBinding
import com.example.trennex.ui.category.adapter.FashionCategoryAdapter
import com.example.trennex.ui.category.adapter.FashionCategoryAdapter.Companion.TYPE_BANNER
import com.example.trennex.ui.category.adapter.FashionCategoryAdapter.Companion.TYPE_HEADER
import com.example.trennex.ui.category.adapter.FashionCategoryAdapter.Companion.TYPE_ITEM
import com.example.trennex.ui.category.model.FashionCategoryModel

class FashionFragment : Fragment() {
    private var _binding : FragmentFashionBinding? = null
    private val binding get()  = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFashionBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = buildList()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if(list[position].Type == TYPE_ITEM) 1 else 3
            }
        }
        binding.rvFashion.layoutManager = grid
        binding.rvFashion.adapter = FashionCategoryAdapter(list)
    }

    private fun buildList(): List<FashionCategoryModel>{
        val list = mutableListOf<FashionCategoryModel>()

        list.add(FashionCategoryModel(TYPE_BANNER))

        list.add(FashionCategoryModel(TYPE_HEADER, "Men's Clothing"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Winter Wear",R.drawable.men_winter))
        list.add(FashionCategoryModel(TYPE_ITEM,"Bottom wear",R.drawable.men_bottom))
        list.add(FashionCategoryModel(TYPE_ITEM,"Top wear",R.drawable.men_top))
        list.add(FashionCategoryModel(TYPE_ITEM,"Ethnic Wear",R.drawable.men_ethic))
        list.add(FashionCategoryModel(TYPE_ITEM,"Tshirts",R.drawable.men_tshirts))
        list.add(FashionCategoryModel(TYPE_ITEM,"Casual Wear",R.drawable.men_casual))
        list.add(FashionCategoryModel(TYPE_ITEM,"Suits",R.drawable.men_suits))
        list.add(FashionCategoryModel(TYPE_ITEM,"Formal wear",R.drawable.men_formal))
        list.add(FashionCategoryModel(TYPE_ITEM,"Sports wear",R.drawable.men_sports))


        list.add(FashionCategoryModel(TYPE_HEADER, "Men's Footwear"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Sport shoes",R.drawable.shoes_sport))
        list.add(FashionCategoryModel(TYPE_ITEM,"Casual shoes",R.drawable.shoes_casual))
        list.add(FashionCategoryModel(TYPE_ITEM,"Shoes",R.drawable.shoes))
        list.add(FashionCategoryModel(TYPE_ITEM,"Sneakers",R.drawable.sneakers))
        list.add(FashionCategoryModel(TYPE_ITEM,"Formal Shoes",R.drawable.formal_shoes))


        list.add(FashionCategoryModel(TYPE_HEADER, "Women's Clothing"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Sarees",R.drawable.women_sarees))
        list.add(FashionCategoryModel(TYPE_ITEM,"Kurtis",R.drawable.women_kurtis))
        list.add(FashionCategoryModel(TYPE_ITEM,"Sweaters",R.drawable.women_sweaters))
        list.add(FashionCategoryModel(TYPE_ITEM,"Jeans",R.drawable.women_jeans))
        list.add(FashionCategoryModel(TYPE_ITEM,"Jackets",R.drawable.women_jackets))
        list.add(FashionCategoryModel(TYPE_ITEM,"Blazer, Coat",R.drawable.women_blazer))
        list.add(FashionCategoryModel(TYPE_ITEM,"TrackSuit",R.drawable.women_tracksuit))
        list.add(FashionCategoryModel(TYPE_ITEM,"Lehenga Choli",R.drawable.women_lehenga))
        list.add(FashionCategoryModel(TYPE_ITEM,"Apparel Sets",R.drawable.women_apparel))


        list.add(FashionCategoryModel(TYPE_HEADER, "Women's Footwear"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Sport Shoes",R.drawable.women_sports))
        list.add(FashionCategoryModel(TYPE_ITEM,"Heels & Flats",R.drawable.women_hills))
        list.add(FashionCategoryModel(TYPE_ITEM,"Slippers",R.drawable.slippers))
        list.add(FashionCategoryModel(TYPE_ITEM,"Boots",R.drawable.women_boots))
        list.add(FashionCategoryModel(TYPE_ITEM,"Sneakers",R.drawable.women_sneakers))


        list.add(FashionCategoryModel(TYPE_HEADER, "Suitcase, Bags & Backpacks"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Backpacks",R.drawable.backpacks))
        list.add(FashionCategoryModel(TYPE_ITEM,"Sling Bags",R.drawable.sling_bags))
        list.add(FashionCategoryModel(TYPE_ITEM,"Travel Bags",R.drawable.travel_bags))
        list.add(FashionCategoryModel(TYPE_ITEM,"Duffel Bags",R.drawable.duffel_bags))


        list.add(FashionCategoryModel(TYPE_HEADER, "Kid's Fashion"))

        list.add(FashionCategoryModel(TYPE_ITEM,"Kid's Styles",R.drawable.kids_style))
        list.add(FashionCategoryModel(TYPE_ITEM,"Kid's Winter",R.drawable.kids_winter))
        list.add(FashionCategoryModel(TYPE_ITEM,"Kid's Sweater",R.drawable.kids_sweater))
        list.add(FashionCategoryModel(TYPE_ITEM,"Kid's Shoes",R.drawable.kids_shoes))
        list.add(FashionCategoryModel(TYPE_ITEM,"Kid's Ethic Sets",R.drawable.kids_ethic))

        return list

    }
}