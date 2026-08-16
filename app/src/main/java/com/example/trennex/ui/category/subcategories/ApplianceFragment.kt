package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentApplianceBinding
import com.example.trennex.ui.category.adapter.ApplianceAdapter
import com.example.trennex.ui.category.adapter.ApplianceAdapter.Companion.TYPE_BANNER
import com.example.trennex.ui.category.adapter.ApplianceAdapter.Companion.TYPE_HEADER
import com.example.trennex.ui.category.adapter.ApplianceAdapter.Companion.TYPE_ITEM
import com.example.trennex.ui.category.model.ApplianceCategoryModel

class ApplianceFragment : Fragment(R.layout.fragment_appliance) {
    private var _binding : FragmentApplianceBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentApplianceBinding.inflate(inflater,container,false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = bundleList()
        val grid = GridLayoutManager(requireContext(),2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return if(list[position].Type == TYPE_ITEM) 1 else 2
            }
        }
        binding.rvAppliance.layoutManager = grid
        binding.rvAppliance.adapter = ApplianceAdapter(list)
    }

    private fun bundleList(): List<ApplianceCategoryModel>{
        val list = mutableListOf<ApplianceCategoryModel>()
        list.add(ApplianceCategoryModel(TYPE_BANNER))
        list.add(ApplianceCategoryModel(TYPE_HEADER,"Shop By Category"))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Television",R.drawable.appliance_tv))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Refrigerators",R.drawable.appliance_refrigerator))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Washing\n"+ "Machines",R.drawable.appliance_washing_machine))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Air\n" + "Conditioners",R.drawable.appliance_air_conditioner))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Kitchen \n" + "Appliances",R.drawable.kitchen_appliances))
        list.add(ApplianceCategoryModel(TYPE_ITEM,"Home\n" + "Appliances",R.drawable.home_appliances))

        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}