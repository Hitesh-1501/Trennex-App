package com.example.trennex.ui.category.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentSportsBinding
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter
import com.example.trennex.ui.category.adapter.SportsCategoryAdapter.Companion.TYPE_SPORTS_ITEM
import com.example.trennex.viewmodel.category.CategoryViewModel

class SportsFragment : Fragment() {
    private var _binding : FragmentSportsBinding? = null
    private val binding get()  = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = viewModel.getSportsItems()
        val grid = GridLayoutManager(requireContext(),3)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return if(list[position].Type == TYPE_SPORTS_ITEM) 1 else 3
            }
        }
        binding.rvSports.layoutManager = grid
        binding.rvSports.adapter = SportsCategoryAdapter(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}