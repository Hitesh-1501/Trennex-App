package com.example.trennex.ui.wishlist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentWishlistBinding
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.WishListStore
import kotlinx.coroutines.launch


class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val wishlistAdapter by lazy { WishlistAdapter(
        emptyList(),
        onItemClicked = {
            val direction = WishlistFragmentDirections.actionWishlistFragmentToProductDetailFragment(it.id)
            findNavController().navigate(direction)
        },
        onAddToCartClicked = {
            CartStore.addItem(
                CartItemModel(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    mrp = it.mrp,
                    price = it.price,
                    rating = it.rating,
                    ratingCount = it.ratingCount,
                    returnPolicy = it.returnPolicy,
                    deliveryDetails = it.deliveryDetails,
                    imageUrl = it.imageUrl,
                    quantity = 1,
                    isSelected = true

                )
            )
            WishListStore.removeItem(it.id)
            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
        },
        onRemoveClicked = {
            WishListStore.removeItem(it.id)
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWishlistBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvwishlist.layoutManager = GridLayoutManager(requireContext(),2)
        binding.rvwishlist.adapter = wishlistAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WishListStore.items.collect { items ->
                    wishlistAdapter.submitList(items)
                    val itemCount = items.size
                    val countLabel = if(itemCount == 1) "1 item" else "$itemCount items"
                    binding.tvWishlistItemCount.text = countLabel
                    binding.emptyStateContainer.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                    binding.nestedScrollView.visibility = if (itemCount == 0) View.GONE else View.VISIBLE
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}