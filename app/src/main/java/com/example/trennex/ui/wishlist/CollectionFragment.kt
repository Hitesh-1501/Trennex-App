package com.example.trennex.ui.wishlist

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.size
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
import androidx.core.view.get
import androidx.core.view.marginEnd
import androidx.core.view.marginTop

class CollectionFragment : Fragment(R.layout.fragment_collection) {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val collectionAdapter by lazy {
        CollectionGridAdapter(
            onCreateCollectionClicked = {openCollectionSelection()},
            onCollectionMenuClicked = { anchor, item -> showCollectionMenu(anchor,item) },
            onCollectionClicked = {openCollectionItem(it)}
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
    private fun openCollectionItem(collection: CollectionModel){
        val direction = CollectionFragmentDirections.actionCollectionFragmentToCollectionItemsFragment(
            collectionId = collection.id,
            collectionName = collection.name
        )
        findNavController().navigate(direction)
    }
    private fun showCollectionMenu(anchor: View,collection: CollectionModel){
        val popup = PopupMenu(ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_TrenNex_PopupMenu),anchor)
        popup.menuInflater.inflate(R.menu.menu_collection_icon,popup.menu)
        popup.setForceShowIcon(true)
        for(index in 0 until popup.menu.size){
            val item = popup.menu[index]
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
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            setHintTextColor(ContextCompat.getColor(requireContext(),R.color.textSecondary))
            setBackgroundResource(R.drawable.edit_text_bg)
        }
        val container = FrameLayout(requireContext()).apply {
            setPadding(20.dpToPx(), 0, 20.dpToPx(), 0)
            addView(editText)
        }

        val dialog = AlertDialog.Builder(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
            .setTitle("Edit Collection")
            .setMessage("Enter a new name for the collection")
            .setView(container)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Save"){_,_->
                CollectionStore.renameCollection(collection.id,editText.text.toString())
                Toast.makeText(requireContext(), "Collection updated", Toast.LENGTH_SHORT).show()
            }
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
    }

    fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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