package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentElectronicsBinding
import com.example.trennex.ui.category.adapter.ElectronicsAdapter
import com.example.trennex.ui.category.adapter.ElectronicsAdapter.Companion.TYPE_ELECTRONICS_ITEM
import com.example.trennex.viewmodel.category.CategoryViewModel

class ElectronicsFragment : Fragment(R.layout.fragment_electronics) {
    private var _binding: FragmentElectronicsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElectronicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = viewModel.getElectronicsItems()
        val grid = GridLayoutManager(requireContext(), 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (list[position].Type == TYPE_ELECTRONICS_ITEM) 1 else 2
            }
        }
        binding.rvElectronics.layoutManager = grid
        binding.rvElectronics.adapter = ElectronicsAdapter(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}