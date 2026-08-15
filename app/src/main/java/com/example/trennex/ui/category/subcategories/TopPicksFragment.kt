package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.databinding.FragmentTopPicksBinding
import com.example.trennex.ui.category.adapter.TopPicksCategoryAdapter
import com.example.trennex.viewmodel.category.CategoryViewModel

class TopPicksFragment : Fragment() {
    private var _binding : FragmentTopPicksBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopPicksBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = viewModel.getTopPicksItems()
        binding.rvFashion.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvFashion.adapter = TopPicksCategoryAdapter(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}