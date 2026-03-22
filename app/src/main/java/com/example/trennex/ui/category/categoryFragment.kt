package com.example.trennex.ui.category

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCategoryBinding
import com.example.trennex.ui.category.adapter.CategorySideBarAdapter
import com.example.trennex.ui.category.model.CategoryModel
import com.example.trennex.ui.category.subcategories.ApplianceFragment
import com.example.trennex.ui.category.subcategories.BeautyFragment
import com.example.trennex.ui.category.subcategories.ElectronicsFragment
import com.example.trennex.ui.category.subcategories.FashionFragment
import com.example.trennex.ui.category.subcategories.FurnitureFragment
import com.example.trennex.ui.category.subcategories.HomeCategoryFragment
import com.example.trennex.ui.category.subcategories.MobileFragment
import com.example.trennex.ui.category.subcategories.TopPicksFragment

class categoryFragment : Fragment(R.layout.fragment_category) {
    private var _binding : FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCategoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        openFragment(TopPicksFragment())
        setUpSideBar()
    }
    fun setUpSideBar(){
        val list = listOf(
            CategoryModel(1, "Top Picks", R.drawable.iv_top_picks),
            CategoryModel(1, "Fashion", R.drawable.iv_fashion),
            CategoryModel(1, "Appliances", R.drawable.iv_appliances),
            CategoryModel(1, "Mobiles", R.drawable.iv_mobiles),
            CategoryModel(1, "Electronics", R.drawable.iv_electronics),
            CategoryModel(1, "Home", R.drawable.iv_home),
            CategoryModel(1, "Beauty", R.drawable.iv_beauty),
            CategoryModel(1, "Furniture", R.drawable.iv_furniture),
            CategoryModel(1, "Toys,Baby,Books", R.drawable.iv_toys),
            CategoryModel(1, "Sports", R.drawable.iv_sport)
        )
        binding.rvCategorySidebar.apply {
            adapter = CategorySideBarAdapter(list){
                when(it.name){
                    "Top Picks" -> openFragment(TopPicksFragment())
                    "Fashion" -> openFragment(FashionFragment())
                    "Appliances" -> openFragment(ApplianceFragment())
                    "Mobiles" -> openFragment(MobileFragment())
                    "Electronics" -> openFragment(ElectronicsFragment())
                    "Home" -> openFragment(HomeCategoryFragment())
                    "Beauty" -> openFragment(BeautyFragment())
                    "Furniture" -> openFragment(FurnitureFragment())
                }
            }
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    private fun openFragment(fragment: Fragment){
       childFragmentManager.beginTransaction()
           .replace(R.id.categoryContentContainer,fragment)
           .commit()
    }
}