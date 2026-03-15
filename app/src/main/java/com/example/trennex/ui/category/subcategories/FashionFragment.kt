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
        setUpRecycleView()
    }

    private  fun setUpRecycleView(){
        val menClothing = listOf(
            FashionCategoryModel("Winter Wear",R.drawable.men_winter),
            FashionCategoryModel("Bottom wear",R.drawable.men_bottom),
            FashionCategoryModel("Top wear",R.drawable.men_top),
            FashionCategoryModel("Ethic Wear",R.drawable.men_ethic),
            FashionCategoryModel("Tshirts",R.drawable.men_tshirts),
            FashionCategoryModel("Casual Wear",R.drawable.men_casual),
            FashionCategoryModel("Suits",R.drawable.men_suits),
            FashionCategoryModel("Formal wear",R.drawable.men_formal),
            FashionCategoryModel("Sports wear",R.drawable.men_sports)
        )

        binding.rvMenClothing.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            adapter = FashionCategoryAdapter(menClothing)
            isNestedScrollingEnabled = false
        }
        val menFootwear = listOf(
            FashionCategoryModel("Sport shoes",R.drawable.shoes_sport),
            FashionCategoryModel("Casual shoes",R.drawable.shoes_casual),
            FashionCategoryModel("Shoes",R.drawable.shoes),
            FashionCategoryModel("Sneakers",R.drawable.sneakers),
            FashionCategoryModel("Formal Shoes",R.drawable.formal_shoes)
        )
        binding.rvMenFootwear.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = FashionCategoryAdapter(menFootwear)
            isNestedScrollingEnabled = false
        }
        val womenClothing = listOf(
            FashionCategoryModel("Sarees",R.drawable.women_sarees),
            FashionCategoryModel("Kurtis",R.drawable.women_kurtis),
            FashionCategoryModel("Sweaters",R.drawable.women_sweaters),
            FashionCategoryModel("Jeans",R.drawable.women_jeans),
            FashionCategoryModel("Jackets",R.drawable.women_jackets),
            FashionCategoryModel("Blazer,Coat",R.drawable.women_blazer),
            FashionCategoryModel("TrackSuit",R.drawable.women_tracksuit),
            FashionCategoryModel("Lehenga Choli",R.drawable.women_lehenga),
            FashionCategoryModel("Apparel Sets",R.drawable.women_apparel),
        )
        binding.rvWomenClothing.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = FashionCategoryAdapter(womenClothing)
            isNestedScrollingEnabled = false
        }

        val womenFootwear = listOf(
            FashionCategoryModel("Sport Shoes",R.drawable.women_sports),
            FashionCategoryModel("Heels & Flats",R.drawable.women_hills),
            FashionCategoryModel("Slippers",R.drawable.slippers),
            FashionCategoryModel("Boots",R.drawable.women_boots),
            FashionCategoryModel("Sneakers",R.drawable.women_sneakers)
        )

        binding.rvWomenFootwear.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = FashionCategoryAdapter(womenFootwear)
            isNestedScrollingEnabled = false
        }
        val backpacks = listOf(
            FashionCategoryModel("Backpacks",R.drawable.backpacks),
            FashionCategoryModel("Sling Bags",R.drawable.sling_bags),
            FashionCategoryModel("Travel Bags",R.drawable.travel_bags),
            FashionCategoryModel("Duffel Bags",R.drawable.duffel_bags)
        )
        binding.rvBags.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = FashionCategoryAdapter(backpacks)
            isNestedScrollingEnabled  = false
        }
        val kids = listOf(
            FashionCategoryModel("Kid's Styles",R.drawable.kids_style),
            FashionCategoryModel("Kid's Winter",R.drawable.kids_winter),
            FashionCategoryModel("Kid's Sweater",R.drawable.kids_sweater),
            FashionCategoryModel("Kid's Shoes",R.drawable.kids_shoes),
            FashionCategoryModel("Kid's Ethic Sets",R.drawable.kids_ethic)
        )
        binding.rvKids.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = FashionCategoryAdapter(kids)
            isNestedScrollingEnabled = false
        }
    }
}