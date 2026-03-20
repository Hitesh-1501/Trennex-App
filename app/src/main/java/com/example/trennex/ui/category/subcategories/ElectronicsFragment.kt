package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentElectronicsBinding
import com.example.trennex.ui.category.adapter.ElectronicsAdapter
import com.example.trennex.ui.category.adapter.ElectronicsAdapter.Companion.TYPE_ELECTRONICS_BANNER
import com.example.trennex.ui.category.adapter.ElectronicsAdapter.Companion.TYPE_ELECTRONICS_ITEM
import com.example.trennex.ui.category.model.ElectronicsCategoryModel

class ElectronicsFragment : Fragment(R.layout.fragment_electronics) {
    private var _binding: FragmentElectronicsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentElectronicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = setUpBundleList()
        val grid = GridLayoutManager(requireContext(),2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return if(list[position].Type == TYPE_ELECTRONICS_ITEM) 1 else 2
            }
        }
        binding.rvElectronics.layoutManager = grid
        binding.rvElectronics.adapter = ElectronicsAdapter(list)
    }

      fun setUpBundleList(): List<ElectronicsCategoryModel> {
        val list = mutableListOf<ElectronicsCategoryModel>()
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_BANNER))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_laptops,"Laptops"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_tab,"Tablets"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronicss_grommings,"Gromming"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_desktop,"Desktop"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_acc,"Mobile Accessories"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_power_banks,"Power Banks"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_gaming,"Gaming"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronic_headphones,"Headphones"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronnic_watches,"Watches"))
        list.add(ElectronicsCategoryModel(TYPE_ELECTRONICS_ITEM,R.drawable.electronics_camera,"Camera"))

        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}