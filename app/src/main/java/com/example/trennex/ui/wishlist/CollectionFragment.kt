package com.example.trennex.ui.wishlist

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCollectionBinding
import com.example.trennex.ui.wishlist.adapter.CollectionGridAdapter
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.viewmodel.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

class CollectionFragment : Fragment(R.layout.fragment_collection) {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WishlistViewModel by viewModels()

    private val collectionAdapter by lazy {
        CollectionGridAdapter(
            onCreateCollectionClicked = { openCollectionSelection() },
            onCollectionMenuClicked = { anchor, item -> showCollectionMenu(anchor, item) },
            onCollectionClicked = { openCollectionItem(it) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCollections.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCollections.adapter = collectionAdapter
        binding.btnCreateCollection.setOnClickListener { openCollectionSelection() }
        observeCollections()

    }

    private fun observeCollections() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) return@collect
                    
                    val collections = state.collections
                    collectionAdapter.submitList(collections)
                    
                    // Always show RV to keep the "Add Collection" item visible
                    binding.rvCollections.visibility = View.VISIBLE
                    
                    val isEmpty = collections.isEmpty()
                    binding.btnCreateCollection.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.ivEmptyCollection.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun openCollectionSelection() {
        findNavController().navigate(R.id.action_collectionFragment_to_collectionSelectionFragment)
    }

    private fun openCollectionItem(collection: CollectionModel) {
        val direction = CollectionFragmentDirections.actionCollectionFragmentToCollectionItemsFragment(
            collectionId = collection.id,
            collectionName = collection.name
        )
        findNavController().navigate(direction)
    }

    private fun showCollectionMenu(anchor: View, collection: CollectionModel) {
        val popup = PopupMenu(ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_TrenNex_PopupMenu), anchor)
        popup.menuInflater.inflate(R.menu.menu_collection_icon, popup.menu)
        popup.setForceShowIcon(true)
        for (index in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(index)
            val title = SpannableString(item.title)
            title.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.textPrimary)),
                0,
                title.length,
                0
            )
            item.title = title
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_collection -> {
                    showRenameCollectionDialog(collection)
                    true
                }
                R.id.action_delete_collection -> {
                    viewModel.deleteCollection(collection.id)
                    Toast.makeText(requireContext(), "Collection deleted", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_share_collection -> {
                    shareCollection(collection)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showRenameCollectionDialog(collection: CollectionModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_collection, null)
        val editText = dialogView.findViewById<EditText>(R.id.etCollectionName)
        editText.setText(collection.name)
        editText.setSelection(collection.name.length)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Collection")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameCollection(collection.id, newName)
                    Toast.makeText(requireContext(), "Collection updated", Toast.LENGTH_SHORT).show()
                }
            }
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
    }

    private fun shareCollection(collection: CollectionModel) {
        val shareText = buildString {
            append("${collection.name}\n")
            append("Items: ${collection.items.size}\n\n")
            collection.items.forEachIndexed { index, item ->
                append("${index + 1}. ${item.title} - ₹${item.price}\n")
            }
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                "Share Collection"
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}