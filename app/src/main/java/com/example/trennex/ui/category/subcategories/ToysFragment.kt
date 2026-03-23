package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentToysBinding
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_ITEM
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_BANNER
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_HEADER
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_ITEM
import com.example.trennex.ui.category.model.ToysCategoryModel


class ToysFragment : Fragment(R.layout.fragment_toys) {
    private var _binding: FragmentToysBinding? =  null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToysBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = listOfBundle()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int{
                return if(list[position].Type == TYPE_FURNITURE_ITEM) 1 else 3
            }
        }
        binding.rvToys.apply {
            adapter = ToysCategoryAdapter(list)
            layoutManager = grid
        }
    }

    private fun listOfBundle(): List<ToysCategoryModel>{
        val list = mutableListOf<ToysCategoryModel>()
        list.add(ToysCategoryModel(TYPE_TOYS_BANNER))

        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.toys_games,"Toys & Games"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.kids_fashion,"Kids Fashion"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.baby,"Baby"))

        list.add(ToysCategoryModel(TYPE_TOYS_HEADER, title = "Stationary"))

        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.school_supplies,"School Supplies"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.pens,"Pens"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.notebooks,"Notebooks"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.art_supplies,"Art Supplies"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.calculator,"Calculator"))
        list.add(ToysCategoryModel(TYPE_TOYS_ITEM,R.drawable.desk_organizer,"Desk Organizer"))


        return list
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}