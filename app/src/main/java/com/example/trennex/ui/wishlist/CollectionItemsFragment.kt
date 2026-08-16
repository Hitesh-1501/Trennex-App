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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCollectionItemsBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.viewmodel.wishlist.WishlistUiState
import com.example.trennex.viewmodel.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

class CollectionItemsFragment : Fragment() {

    private var _binding: FragmentCollectionItemsBinding? = null
    private val binding get() = _binding!!

    private val args : CollectionItemsFragmentArgs by navArgs()
    private val viewModel: WishlistViewModel by viewModels()

    private var collectionName: String = ""
    private var collectionCount: Int = 0
    private var didPopAfterDelete = false

    private val itemsAdapter by lazy {
        WishlistAdapter(
            items = emptyList(),
            onItemClicked = {
                val direction = CollectionItemsFragmentDirections.actionCollectionItemsFragmentToProductDetailFragment(it.id)
                findNavController().navigate(direction)
            },
            onAddToCartClicked = {
                viewModel.addToCart(it)
                viewModel.removeItemsFromCollection(args.collectionId, listOf(it.id))
                Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
            },
            onRemoveClicked = {
                viewModel.removeItemsFromCollection(args.collectionId, listOf(it.id))
            },
            onSelectionChanged = {},
            onItemSelectionToggled = { viewModel.toggleItemSelection(it) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressForSelectionMode()
        binding.rvCollectionItems.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCollectionItems.adapter = itemsAdapter

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val collections = state.collections
                    val target = collections.firstOrNull { it.id == args.collectionId }
                    if (target == null) {
                        if (!didPopAfterDelete && findNavController().currentDestination?.id == R.id.collectionItemsFragment) {
                            didPopAfterDelete = true
                            findNavController().popBackStack()
                        }
                        return@collect
                    }
                    
                    collectionName = target.name
                    collectionCount = target.items.size
                    
                    itemsAdapter.submitList(target.items)
                    itemsAdapter.setSelectionMode(state.isSelectionMode)
                    itemsAdapter.setSelectedIds(state.selectedItemIds)
                    
                    binding.emptyStateContainer.visibility = if (collectionCount == 0) View.VISIBLE else View.GONE
                    binding.rvCollectionItems.visibility = if (collectionCount == 0) View.GONE else View.VISIBLE
                    
                    updateToolbar(state)
                }
            }
        }
    }

    private fun updateToolbar(state: WishlistUiState) {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val backArrow = toolbarRoot.findViewById<ImageView>(R.id.back_arrow) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val subtitleText = toolbarRoot.findViewById<TextView>(R.id.page_subtitle) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        if (state.isSelectionMode) {
            titleText.text = "${state.selectedCount} Items Selected"
            subtitleText.visibility = View.GONE
            editDeleteIcon.setImageResource(R.drawable.wishlist_delete)
            cartShareIcon.setImageResource(R.drawable.wishlist_share)
            
            val enableActions = state.selectedCount > 0
            editDeleteIcon.isEnabled = enableActions
            cartShareIcon.isEnabled = enableActions
            editDeleteIcon.alpha = if (enableActions) 1f else 0.4f
            cartShareIcon.alpha = if (enableActions) 1f else 0.4f
        } else {
            titleText.text = collectionName
            subtitleText.visibility = View.VISIBLE
            subtitleText.text = if (collectionCount == 1) "1 item" else "$collectionCount items"
            editDeleteIcon.setImageResource(R.drawable.ic_edit)
            cartShareIcon.setImageResource(R.drawable.cart)
            
            val hasItems = collectionCount > 0
            editDeleteIcon.isEnabled = hasItems
            cartShareIcon.isEnabled = hasItems
            editDeleteIcon.alpha = if (hasItems) 1f else 0.4f
            cartShareIcon.alpha = if (hasItems) 1f else 0.4f
        }

        editDeleteIcon.setOnClickListener {
            if (!state.isSelectionMode) {
                viewModel.toggleSelectionMode(true)
            } else {
                showDeleteConfirmationDialog(state.selectedItemIds.toList())
            }
        }
        
        cartShareIcon.setOnClickListener {
            if (state.isSelectionMode) {
                shareSelectedCollectionItems(state)
            } else if (findNavController().currentDestination?.id == R.id.collectionItemsFragment) {
                findNavController().navigate(R.id.action_collectionItemsFragment_to_cartFragment)
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

    private fun showDeleteConfirmationDialog(selectedIds: List<Int>) {
        val count = selectedIds.size
        if (count == 0) return

        val itemLabel = if (count == 1) "item" else "items"
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove From Collection")
            .setMessage("Are you sure want to remove $count $itemLabel from this collection.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeItemsFromCollection(args.collectionId, selectedIds)
                viewModel.toggleSelectionMode(false)
            }
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)
    }

    private fun shareSelectedCollectionItems(state: WishlistUiState) {
        val targetCollection = state.collections.firstOrNull { it.id == args.collectionId } ?: return
        val selectedItems = targetCollection.items.filter { state.selectedItemIds.contains(it.id) }
        if (selectedItems.isEmpty()) return

        val shareText = buildString {
            append("$collectionName - selected items:\n")
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
                "Share Collection Items"
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}