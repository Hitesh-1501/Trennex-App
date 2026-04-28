package com.example.trennex.ui.wishlist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCollectionItemsBinding
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.utils.cart.CartStore
import com.example.trennex.utils.wishlist.CollectionStore
import kotlinx.coroutines.launch


class CollectionItemsFragment : Fragment() {

    private var _binding: FragmentCollectionItemsBinding? = null
    private val binding get() = _binding!!

    private val args : CollectionItemsFragmentArgs by navArgs()

    private var collectionName: String = ""
    private var collectionCount : Int = 0
    private var didPopAfterDelete = false

    private val itemsAdapter by lazy {
        WishlistAdapter(
            items = emptyList(),
            onItemClicked = {
                val direction = CollectionItemsFragmentDirections.actionCollectionItemsFragmentToProductDetailFragment(it.id)
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
                CollectionStore.removeItemsFromCollection(args.collectionId,listOf(it.id))
                Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
            },
            onRemoveClicked = {
                CollectionStore.removeItemsFromCollection(args.collectionId,listOf(it.id))
            },
            onSelectionChanged = {selectedCount ->
                updateSelectionToolbarState(selectedCount)
            }
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCollectionItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupBackPressForSelectionMode()
        binding.rvCollectionItems.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCollectionItems.adapter = itemsAdapter

        observeCollectionItems()
    }

    private fun setupToolbar(){
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val backArrow = toolbarRoot.findViewById<ImageView>(R.id.back_arrow) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val subtitleText = toolbarRoot.findViewById<TextView>(R.id.page_subtitle) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        collectionName = args.collectionName
        titleText.text = collectionName
        subtitleText.visibility = View.VISIBLE
        subtitleText.text = "0 items"

        editDeleteIcon.setOnClickListener {
            if(!itemsAdapter.isSelectionMode()){
                itemsAdapter.setSelectionMode(true)
                titleText.text = "Items Selected"
                subtitleText.visibility = View.GONE
                editDeleteIcon.setImageResource(R.drawable.wishlist_delete)
                cartShareIcon.setImageResource(R.drawable.wishlist_share)
                updateSelectionToolbarState(0)
            }else{
                showDeleteConfirmationDialog()
            }
        }
        cartShareIcon.setOnClickListener {
            if (itemsAdapter.isSelectionMode()) {
                shareSelectedCollectionItems()
            } else if (findNavController().currentDestination?.id == R.id.collectionItemsFragment) {
                findNavController().navigate(R.id.action_collectionItemsFragment_to_cartFragment)
            }
        }

        backArrow.setOnClickListener {
            if (itemsAdapter.isSelectionMode()) {
                exitSelectionMode()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun observeCollectionItems(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                CollectionStore.collections.collect {collections ->
                    val target = collections.firstOrNull { it.id == args.collectionId}
                    if(target == null){
                        if(!didPopAfterDelete && findNavController().currentDestination?.id == R.id.collectionItemsFragment){
                            didPopAfterDelete = true
                            findNavController().popBackStack()
                        }
                        return@collect
                    }
                    collectionName = target.name
                    collectionCount = target.items.size
                    itemsAdapter.submitList(target.items)
                    binding.emptyStateContainer.visibility = if(collectionCount == 0)View.VISIBLE else View.GONE
                    binding.rvCollectionItems.visibility = if (collectionCount == 0) View.GONE else View.VISIBLE
                    updateDefaultToolbarState(collectionCount)
                }
            }
        }
    }

    private fun setupBackPressForSelectionMode() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (itemsAdapter.isSelectionMode()) {
                exitSelectionMode()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
    private fun updateDefaultToolbarState(itemCount: Int) {
        if (itemsAdapter.isSelectionMode()) return
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val subtitleText = toolbarRoot.findViewById<TextView>(R.id.page_subtitle) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        titleText.text = collectionName
        subtitleText.visibility = View.VISIBLE
        subtitleText.text = if (itemCount == 1) "1 item" else "$itemCount items"

        editDeleteIcon.isEnabled = itemCount > 0
        cartShareIcon.isEnabled = itemCount > 0
        editDeleteIcon.alpha = if (itemCount > 0) 1f else 0.4f
        cartShareIcon.alpha = if (itemCount > 0) 1f else 0.4f
    }

    private fun updateSelectionToolbarState(selectedCount: Int) {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return
        if (!itemsAdapter.isSelectionMode()) return

        val enableActions = selectedCount > 0
        editDeleteIcon.isEnabled = enableActions
        cartShareIcon.isEnabled = enableActions
        editDeleteIcon.alpha = if (enableActions) 1f else 0.4f
        cartShareIcon.alpha = if (enableActions) 1f else 0.4f
    }
    private fun exitSelectionMode() {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        val titleText = toolbarRoot.findViewById<TextView>(R.id.page_title) ?: return
        val subtitleText = toolbarRoot.findViewById<TextView>(R.id.page_subtitle) ?: return
        val editDeleteIcon = toolbarRoot.findViewById<ImageView>(R.id.ivedit) ?: return
        val cartShareIcon = toolbarRoot.findViewById<ImageView>(R.id.ivcart) ?: return

        itemsAdapter.setSelectionMode(false)
        itemsAdapter.clearSelection()
        titleText.text = collectionName
        subtitleText.visibility = View.VISIBLE
        subtitleText.text = if (collectionCount == 1) "1 item" else "$collectionCount items"
        editDeleteIcon.setImageResource(R.drawable.ic_edit)
        cartShareIcon.setImageResource(R.drawable.cart)
        updateDefaultToolbarState(collectionCount)
    }

    private fun showDeleteConfirmationDialog() {
        val selectedItems = itemsAdapter.getSelectedItems()
        val count = selectedItems.size
        if (selectedItems.isEmpty()) return

        val itemLabel = if (count == 1) "item" else "items"
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove From Collection")
            .setMessage("Are you sure want to remove $count $itemLabel from this collection.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                CollectionStore.removeItemsFromCollection(args.collectionId, selectedItems.map { it.id })
                exitSelectionMode()
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

    private fun shareSelectedCollectionItems() {
        val selectedItems = itemsAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) return

        val shareText = buildString {
            append("$collectionName - selected items:\n")
            selectedItems.forEachIndexed { index, item ->
                append("${index + 1}. ${item.title} - ₹${item.price}\\n")
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