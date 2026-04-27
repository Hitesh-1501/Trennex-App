package com.example.trennex.ui.wishlist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCollectionBinding
import com.example.trennex.ui.wishlist.adapter.CollectionGridAdapter
import com.example.trennex.ui.wishlist.model.CollectionModel
import com.example.trennex.utils.wishlist.CollectionStore
import kotlinx.coroutines.launch
import java.security.cert.TrustAnchor
import kotlin.random.Random

class CollectionFragment : Fragment(R.layout.fragment_collection) {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val collectionAdapter by lazy {
        CollectionGridAdapter(
            onCreateCollectionClicked = {openCollectionSelection()},
            onCollectionMenuClicked = { anchor, item -> showCollectionMenu(anchor,item) }
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

    private fun observeCollections(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                CollectionStore.collections.collect {
                    collectionAdapter.submitList(it)
                    val isEmpty = it.isEmpty()
                    binding.btnCreateCollection.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.ivEmptyCollection.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.rvCollections.visibility = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }
    private fun openCollectionSelection(){
        findNavController().navigate(R.id.action_collectionFragment_to_collectionSelectionFragment)
    }
    private fun showCollectionMenu(anchor: View,collection: CollectionModel){
        val popup = PopupMenu(requireContext(),anchor)
        popup.menuInflater.inflate(R.menu.menu_collection_icon,popup.menu)
        popup.setForceShowIcon(true)
        popup.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId){
                R.id.action_edit_collection ->{
                    showRenameCollectionDialog(collection)
                    true
                }
                R.id.action_delete_collection -> {
                    CollectionStore.removeCollection(collection.id)
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
    private fun showRenameCollectionDialog(collection: CollectionModel){
        val editText = EditText(requireContext()).apply {
            setText(collection.name)
            setPadding(40, 30, 40, 20)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Collection")
            .setView(editText)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Save"){_,_->
                CollectionStore.renameCollection(collection.id,editText.text.toString())
                Toast.makeText(requireContext(), "Collection updated", Toast.LENGTH_SHORT).show()
            }
            .show()
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