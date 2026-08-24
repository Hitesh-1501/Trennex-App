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
import com.example.trennex.viewmodel.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private var wishlistCount: Int = 0

    private val viewModel: WishlistViewModel by viewModels()

    private val wishlistAdapter by lazy { WishlistAdapter(
        emptyList(),
        onItemClicked = {
            val direction = WishlistFragmentDirections.actionWishlistFragmentToProductDetailFragment(it.id)
            findNavController().navigate(direction)
        },
        onAddToCartClicked = {
            viewModel.addItemToCart(it)
            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
        },
        onRemoveClicked = {
            viewModel.removeItemFromWishlist(it.id)
        },
        onSelectionChanged = { selectedCount ->
            updateSelectionToolbarState(selectedCount)
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
        setupWishlistToolbar()
        setupBackPressForSelectionMode()
        binding.rvwishlist.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvwishlist.adapter = wishlistAdapter

        binding.btnMyCollection.setOnClickListener {
            if (binding.btnMyCollection.isEnabled && findNavController().currentDestination?.id == R.id.wishlistFragment) {
                findNavController().navigate(R.id.action_wishlistFragment_to_collectionFragment)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) return@collect

                    val items = state.items
                    wishlistAdapter.submitList(items)
                    val itemCount = items.size
                    wishlistCount = itemCount
                    val countLabel = if (itemCount == 1) "1 item" else "$itemCount items"
                    binding.tvWishlistItemCount.text = countLabel
                    binding.emptyStateContainer.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                    binding.nestedScrollView.visibility = if (itemCount == 0) View.GONE else View.VISIBLE
                    updateDefaultToolbarState(itemCount)
                    updateCollectionButtonState(itemCount)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupWishlistToolbar()
    }

    private fun setupWishlistToolbar() {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val backArrow = toolbarRoot.findViewById<ImageView>(R.id.back_arrow) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        if (wishlistAdapter.isSelectionMode()) {
            val selectedCount = wishlistAdapter.getSelectedItems().size
            titleText.text = if (selectedCount > 0) "$selectedCount Items Selected" else "Items Selected"
            editDeleteIcon.setImageResource(R.drawable.wishlist_delete)
            cartShareIcon.setImageResource(R.drawable.wishlist_share)
            updateSelectionToolbarState(selectedCount)
        } else {
            titleText.text = "Wishlist"
            editDeleteIcon.setImageResource(R.drawable.ic_edit)
            cartShareIcon.setImageResource(R.drawable.cart)
            updateDefaultToolbarState(wishlistCount)
        }

        editDeleteIcon.setOnClickListener {
            if (!wishlistAdapter.isSelectionMode()) {
                wishlistAdapter.setSelectionMode(true)
                titleText.text = "Items Selected"
                editDeleteIcon.setImageResource(R.drawable.wishlist_delete)
                cartShareIcon.setImageResource(R.drawable.wishlist_share)
                updateSelectionToolbarState(0)
            } else {
                showDeleteConfirmationDialog()
            }
        }

        cartShareIcon.setOnClickListener {
            if (wishlistAdapter.isSelectionMode()) {
                shareSelectedWishlistItems()
            } else if (findNavController().currentDestination?.id == R.id.wishlistFragment) {
                findNavController().navigate(R.id.action_wishlistFragment_to_cartFragment)
            }
        }

        backArrow.setOnClickListener {
            if (wishlistAdapter.isSelectionMode()) {
                existSelectionMode()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupBackPressForSelectionMode() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (wishlistAdapter.isSelectionMode()) {
                existSelectionMode()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun updateSelectionToolbarState(selectedCount: Int) {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        if (!wishlistAdapter.isSelectionMode()) {
            return
        }

        titleText.text = if (selectedCount > 0) "$selectedCount Items Selected" else "Items Selected"

        val enableActions = selectedCount > 0
        editDeleteIcon.isEnabled = enableActions
        cartShareIcon.isEnabled = enableActions
        editDeleteIcon.alpha = if (enableActions) 1f else 0.4f
        cartShareIcon.alpha = if (enableActions) 1f else 0.4f
    }

    private fun existSelectionMode() {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return
        wishlistAdapter.setSelectionMode(false)
        wishlistAdapter.clearSelection()
        titleText.text = "Wishlist"
        editDeleteIcon.setImageResource(R.drawable.ic_edit)
        cartShareIcon.setImageResource(R.drawable.cart)
        editDeleteIcon.isEnabled = wishlistCount > 0
        cartShareIcon.isEnabled = wishlistCount > 0
        editDeleteIcon.alpha = if (wishlistCount > 0) 1f else 0.4f
        cartShareIcon.alpha = if (wishlistCount > 0) 1f else 0.4f
    }

    private fun updateDefaultToolbarState(itemCount: Int) {
        if (wishlistAdapter.isSelectionMode()) return
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return
        editDeleteIcon.isEnabled = itemCount > 0
        cartShareIcon.isEnabled = itemCount > 0
        editDeleteIcon.alpha = if (itemCount > 0) 1f else 0.4f
        cartShareIcon.alpha = if (itemCount > 0) 1f else 0.4f
    }

    private fun updateCollectionButtonState(itemCount: Int) {
        val enabled = itemCount > 0
        binding.btnMyCollection.isEnabled = enabled
        binding.btnMyCollection.alpha = if (enabled) 1f else 0.4f
        binding.ivCollectionIcon.alpha = if (enabled) 1f else 0.4f
        binding.tvCollectionLabel.alpha = if (enabled) 1f else 0.4f
    }

    private fun showDeleteConfirmationDialog() {
        val selectedItems = wishlistAdapter.getSelectedItems()
        val count = selectedItems.size
        val msg = if (count == 1) "item" else "items"
        if (selectedItems.isEmpty()) return
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove From Wishlist")
            .setMessage("Are you sure want to remove $count $msg from your wishlist.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                selectedItems.forEach {
                    viewModel.removeItemFromWishlist(it.id)
                }
                existSelectionMode()
            }
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
        dialog.findViewById<TextView>(com.google.android.material.R.id.alertTitle)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )

    }

    fun shareSelectedWishlistItems() {
        val selectedItems = wishlistAdapter.getSelectedItems()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}