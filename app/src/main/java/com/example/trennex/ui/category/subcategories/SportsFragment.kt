package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentSportsBinding
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_ITEM
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter.Companion.TYPE_SPORTS_BANNER
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter.Companion.TYPE_SPORTS_HEADER
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter.Companion.TYPE_SPORTS_ITEM
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_BANNER
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_HEADER
import com.example.trennex.ui.category.adapter.ToysCategoryAdapter.Companion.TYPE_TOYS_ITEM
import com.example.trennex.ui.category.model.SportsCategoryModel
import com.example.trennex.ui.category.model.ToysCategoryModel

class SportsFragment : Fragment(R.layout.fragment_sports){
    private var _binding : FragmentSportsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentSportsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = listOfBundle()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int{
                return if(list[position].Type == TYPE_SPORTS_ITEM) 1 else 3
            }
        }
        binding.rvSports.apply {
            adapter = SportsCategoryAdapter(list)
            layoutManager = grid
        }
    }

    private fun listOfBundle(): List<SportsCategoryModel>{
        val list = mutableListOf<SportsCategoryModel>()
        list.add(SportsCategoryModel(TYPE_SPORTS_BANNER))

        list.add(SportsCategoryModel(TYPE_SPORTS_HEADER, title = "Sports"))

        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.sports_cycle,"Cycles"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.sports_bike,"Exercise Bike"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.sports_waklingpad,"Walking Pad"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.fitnes_accessories,"Fitness Accessories"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.cricket,"Cricket"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.badminton,"Badminton"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.skating,"Skating"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.yoga,"Yoga"))
        list.add(SportsCategoryModel(TYPE_SPORTS_ITEM,R.drawable.team_sport,"Team Sport"))



        return list
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}