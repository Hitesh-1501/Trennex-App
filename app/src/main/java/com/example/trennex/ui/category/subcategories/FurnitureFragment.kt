package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentFurnitureBinding
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter
import com.example.trennex.ui.category.adapter.BeautyCategoryAdapter.Companion.TYPE_BEAUTY_ITEM
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_BANNER
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_HEADER
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_ITEM
import com.example.trennex.ui.category.model.FurnitureCategoryModel

class FurnitureFragment : Fragment() {
    private var _binding : FragmentFurnitureBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFurnitureBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = bundleList()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int{
                return if(list[position].Type == TYPE_BEAUTY_ITEM) 1 else 3
            }
        }
        binding.rvFurniture.apply {
            adapter = FurnitureCategoryAdapter(list)
            layoutManager = grid
        }
    }

    private fun bundleList(): List<FurnitureCategoryModel>{
        val list = mutableListOf<FurnitureCategoryModel>()
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_BANNER))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_HEADER, title = "Bedroom Essentials"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.beds,"Beds"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.mattresses,"Mattresses"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.wardrobes,"Wardrobes"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.collapsible_wardrobes,"Collapsible Wardrobes"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.dressing_tables,"Dressing Tables"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.side_tables,"Side Tables"))


        list.add(FurnitureCategoryModel(TYPE_FURNITURE_HEADER, title = "Living Room Essentials"))

        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.recliners,"Recliners"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.tv_units,"Tv Units"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.shoes_racks,"Shoe Racks"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.coffee_tables,"Coffee Tables"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.home_temples,"Home Temples"))


        list.add(FurnitureCategoryModel(TYPE_FURNITURE_HEADER, title = "Study & Office Essentials"))

        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.office_chair,"Office & Study Chairs"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.study_tables,"Office & Study Tables"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.laptop_tables,"Laptop Tables"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.bookshelves,"Bookshelves"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.gaming_chairs,"Gaming Chairs"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.cabinets_drawers,"Cabinets & drawers"))



        list.add(FurnitureCategoryModel(TYPE_FURNITURE_HEADER, title = "Dinning & Kitchen"))

        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.dinning_sets,"Dining Sets"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.kitchen_cabinets,"Kitchen Cabinets"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.dinning_tables,"Dining Tables"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.kitchen_trolley,"Kitchen Trolley"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.dinning_chairs,"Dining Chairs"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.bar_stools,"Bar Stools Chairs"))

        list.add(FurnitureCategoryModel(TYPE_FURNITURE_HEADER, title = "Outdoor Furniture"))

        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.chairs,"Chairs"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.outdoor_sets,"Outdoor Sets"))
        list.add(FurnitureCategoryModel(TYPE_FURNITURE_ITEM, R.drawable.hammock_swings,"Hammock Swings"))

        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}