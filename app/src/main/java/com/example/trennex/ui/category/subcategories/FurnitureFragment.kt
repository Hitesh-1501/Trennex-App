package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentFurnitureBinding
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter
import com.example.trennex.ui.category.adapter.FurnitureCategoryAdapter.Companion.TYPE_FURNITURE_ITEM
import com.example.trennex.viewmodel.category.CategoryViewModel

class FurnitureFragment : Fragment() {
    private var _binding: FragmentFurnitureBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFurnitureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = viewModel.getFurnitureItems()
        val grid = GridLayoutManager(requireContext(), 3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (list[position].Type == TYPE_FURNITURE_ITEM) 1 else 3
            }
        }
        binding.rvFurniture.layoutManager = grid
        binding.rvFurniture.adapter = FurnitureCategoryAdapter(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}