package com.example.trennex.ui.wishlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.DialogCreateCollectionBinding
import com.example.trennex.databinding.FragmentCollectionSelectionBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.wishlist.adapter.WishlistAdapter
import com.example.trennex.viewmodel.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

class CollectionSelectionFragment : Fragment(R.layout.fragment_collection_selection) {
    private var _binding: FragmentCollectionSelectionBinding? = null
    private val binding get() = _binding!!

    private var selectedCount: Int = 0

    private var backArrow: ImageView? = null
    private var titleText: TextView? = null
    private var subtitleText: TextView? = null
    private var addText: TextView? = null
    
    private val viewModel: WishlistViewModel by viewModels()

    private val selectionAdapter by lazy {
        WishlistAdapter(
            items = emptyList(),
            onItemClicked = {},
            onAddToCartClicked = {},
            onRemoveClicked = {},
            onSelectionChanged = {
                selectedCount = it
                updateSelectionToolbar()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbarFromMainActivity()
        binding.rvWishlistSelection.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvWishlistSelection.adapter = selectionAdapter
        selectionAdapter.setSelectionMode(true)
        selectionAdapter.clearSelection()
        
        addText?.setOnClickListener {
            if (selectionAdapter.getSelectedItems().isNotEmpty()) {
                showCreateCollectionDialog()
            }
        }
        
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
        
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) return@collect
                    selectionAdapter.submitList(state.items)
                }
            }
        }
    }

    private fun setUpToolbarFromMainActivity() {
        val toolbarRoot = (requireActivity() as? MainActivity)?.findViewById<View>(R.id.toolbarContainer) ?: return
        backArrow = toolbarRoot.findViewById(R.id.back_arrow)
        titleText = toolbarRoot.findViewById(R.id.page_title)
        subtitleText = toolbarRoot.findViewById(R.id.page_subtitle)
        addText = toolbarRoot.findViewById(R.id.tvAdd)

        titleText?.text = "Items Selected"
        subtitleText?.text = "0 items"
        addText?.isEnabled = false
        addText?.alpha = 0.4f
        backArrow?.setOnClickListener { findNavController().popBackStack() }
    }
    private fun updateSelectionToolbar() {
        subtitleText?.text = if (selectedCount == 1) "1 item" else "$selectedCount items"
        addText?.isEnabled = selectedCount > 0
        addText?.alpha = if (selectedCount > 0) 1f else 0.4f
    }
    private fun showCreateCollectionDialog() {
        val selectedItems = selectionAdapter.getSelectedItems()
        val dialogBinding = DialogCreateCollectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        addSelectedPreview(dialogBinding.selectedPreviewContainer, selectedItems.map { it.imageUrl })
        dialogBinding.btnConfirmCreate.setOnClickListener {
            val name = dialogBinding.etCollectionName.text?.toString().orEmpty()
            viewModel.createCollection(name, selectedItems)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Collection created", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        dialog.show()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addSelectedPreview(container: LinearLayout, urls: List<String>) {
        container.removeAllViews()
        urls.forEach { url ->
            val image = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(180, 180).apply { marginEnd = 10 }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = requireContext().getDrawable(R.drawable.bg_preview_blue)
            }
            Glide.with(image)
                .load(url)
                .error(R.drawable.placeholder)
                .into(image)
            container.addView(image)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backArrow = null
        titleText = null
        subtitleText = null
        addText = null
        _binding = null
    }
}