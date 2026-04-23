package com.example.trennex.ui.wishlist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentWishlistBinding
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import kotlinx.coroutines.launch


class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val wishlistAdapter by lazy { WishlistAdapter(emptyList()) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWishlistBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvwishlist.adapter = wishlistAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WishListStore.items.collect { items ->
                    wishlistAdapter.submitList(items)
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}