package com.example.trennex.ui.wishlist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentWishlistBinding
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.ui.wishlist.model.WishlistItemsModel


class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding : FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWishlistBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycleView()
    }

    private fun setUpRecycleView(){
        val items  = listOf(
            WishlistItemsModel(1,R.drawable.samsung_transperent,"Samsung S24 Onyx Black","40,999"),
            WishlistItemsModel(1,R.drawable.trennex_tshirt,"TreNex Men's solid white tshirt","499"),
            WishlistItemsModel(1,R.drawable.lenovo_laptop,"Lenovo Yoga i5 laptop","51,999"),
        )

        binding.rvwishlist.layoutManager = GridLayoutManager(requireContext(),2)
        binding.rvwishlist.adapter = WishlistAdapter(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}