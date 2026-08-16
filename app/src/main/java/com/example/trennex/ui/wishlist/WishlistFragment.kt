package com.example.trennex.ui.wishlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentWishlistBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.viewmodel.wishlist.WishlistUiState
import com.example.trennex.viewmodel.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WishlistViewModel by viewModels()

    private val wishlistAdapter by lazy { WishlistAdapter(
        emptyList(),
        onItemClicked = {
            val direction = WishlistFragmentDirections.actionWishlistFragmentToProductDetailFragment(it.id)
            findNavController().navigate(direction)
        },
        onAddToCartClicked = {
            viewModel.addToCart(it)
            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
        },
        onRemoveClicked = {
            viewModel.removeItem(it.id)
        },
        onSelectionChanged = { _ ->
            // Handled via ViewModel now, but callback kept for adapter compatibility if needed
        },
        onItemSelectionToggled = { itemId ->
            viewModel.toggleItemSelection(itemId)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressForSelectionMode()
        binding.rvwishlist.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvwishlist.adapter = wishlistAdapter

        binding.btnMyCollection.setOnClickListener {
            if (binding.btnMyCollection.isEnabled && findNavController().currentDestination?.id == R.id.wishlistFragment) {
                findNavController().navigate(R.id.action_wishlistFragment_to_collectionFragment)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: WishlistUiState) {
        wishlistAdapter.submitList(state.items)
        wishlistAdapter.setSelectionMode(state.isSelectionMode)
        wishlistAdapter.setSelectedIds(state.selectedItemIds)
        
        val itemCount = state.itemCount
        val countLabel = if (itemCount == 1) "1 item" else "$itemCount items"
        binding.tvWishlistItemCount.text = countLabel
        binding.emptyStateContainer.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
        binding.nestedScrollView.visibility = if (itemCount == 0) View.GONE else View.VISIBLE
        
        updateToolbar(state)
        updateCollectionButtonState(itemCount)
    }

    private fun updateToolbar(state: WishlistUiState) {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val backArrow = toolbarRoot.findViewById<ImageView>(R.id.back_arrow) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        if (state.isSelectionMode) {
            titleText.text = "${state.selectedCount} Items Selected"
            editDeleteIcon.setImageResource(R.drawable.wishlist_delete)
            cartShareIcon.setImageResource(R.drawable.wishlist_share)
            
            val enableActions = state.selectedCount > 0
            editDeleteIcon.isEnabled = enableActions
            cartShareIcon.isEnabled = enableActions
            editDeleteIcon.alpha = if (enableActions) 1f else 0.4f
            cartShareIcon.alpha = if (enableActions) 1f else 0.4f
        } else {
            titleText.text = "Wishlist"
            editDeleteIcon.setImageResource(R.drawable.ic_edit)
            cartShareIcon.setImageResource(R.drawable.cart)
            
            val hasItems = state.itemCount > 0
            editDeleteIcon.isEnabled = hasItems
            cartShareIcon.isEnabled = hasItems
            editDeleteIcon.alpha = if (hasItems) 1f else 0.4f
            cartShareIcon.alpha = if (hasItems) 1f else 0.4f
        }

        editDeleteIcon.setOnClickListener {
            if (!state.isSelectionMode) {
                viewModel.toggleSelectionMode(true)
            } else {
                showDeleteConfirmationDialog(state.selectedCount)
            }
        }
        
        cartShareIcon.setOnClickListener {
            if (state.isSelectionMode) {
                shareSelectedWishlistItems(state)
            } else if (findNavController().currentDestination?.id == R.id.wishlistFragment) {
                findNavController().navigate(R.id.action_wishlistFragment_to_cartFragment)
            }
        }
        
        backArrow.setOnClickListener {
            if (state.isSelectionMode) {
                viewModel.toggleSelectionMode(false)
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupBackPressForSelectionMode() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewModel.uiState.value.isSelectionMode) {
                viewModel.toggleSelectionMode(false)
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun updateCollectionButtonState(itemCount: Int) {
        val enabled = itemCount > 0
        binding.btnMyCollection.isEnabled = enabled
        binding.btnMyCollection.alpha = if (enabled) 1f else 0.4f
        binding.ivCollectionIcon.alpha = if (enabled) 1f else 0.4f
        binding.tvCollectionLabel.alpha = if (enabled) 1f else 0.4f
    }

    private fun showDeleteConfirmationDialog(count: Int) {
        if (count == 0) return
        val msg = if (count == 1) "item" else "items"
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove From Wishlist")
            .setMessage("Are you sure want to remove $count $msg from your wishlist.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeSelectedItems()
            }
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)
        
        // Custom styling for dialog buttons if needed to match original
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
    }

    private fun shareSelectedWishlistItems(state: WishlistUiState) {
        val selectedItems = state.items.filter { state.selectedItemIds.contains(it.id) }
        if (selectedItems.isEmpty()) return
        val shareText = buildString {
            append("My selected wishlist items:\n")
            selectedItems.forEachIndexed { index, item ->
                append("${index + 1}. ${item.title} - ₹${item.price}\n")
            }
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                "Share Wishlist items"
            )
        )
    }

    override fun onResume() {
        super.onResume()
        // Ensure toolbar is updated when returning to fragment
        render(viewModel.uiState.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}