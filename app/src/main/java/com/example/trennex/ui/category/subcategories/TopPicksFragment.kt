package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentTopPicksBinding
import com.example.trennex.ui.category.adapter.TopPicksCategoryAdapter
import com.example.trennex.ui.category.model.SubCategoryModel

class TopPicksFragment : Fragment(R.layout.fragment_top_picks) {
    private var _binding : FragmentTopPicksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTopPicksBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycleView()
    }

    private fun setUpRecycleView(){
        val topPicks = listOf(
            SubCategoryModel(1, "Fashion", R.drawable.categories_fashion),
            SubCategoryModel(1, "Mobiles", R.drawable.categories_mobile),
            SubCategoryModel(1, "Electronics", R.drawable.categories_electronics),
            SubCategoryModel(1, "Appliance", R.drawable.categories_appliance),
            SubCategoryModel(1, "Home", R.drawable.categories_home),
            SubCategoryModel(1, "Beauty", R.drawable.categories_beauty),
            SubCategoryModel(1, "Furniture", R.drawable.categories_furniture),
            SubCategoryModel(1, "Toys", R.drawable.category_toys),
            SubCategoryModel(1, "Sports", R.drawable.category_sports)
        )
        binding.rvFashion.apply {
            layoutManager = GridLayoutManager(requireContext(),4)
            adapter = TopPicksCategoryAdapter(topPicks)
        }
    }
}