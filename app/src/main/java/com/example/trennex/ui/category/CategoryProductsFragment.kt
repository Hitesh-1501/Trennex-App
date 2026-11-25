package com.example.trennex.ui.category

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.databinding.FragmentCategoryProductsBinding
import com.example.trennex.databinding.SheetSearchSortBinding
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.common.NetworkStatus
import com.example.trennex.ui.search.SearchSortOrder
import com.example.trennex.ui.search.adapter.SearchResultAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class CategoryProductsFragment : Fragment(R.layout.fragment_category_products) {
    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private var products = emptyList<ProductResponse>()
    private var sortOrder = SearchSortOrder.RELEVANCE
    private var loadJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoryProductsBinding.bind(view)
        val slug = requireArguments().getString(ARG_CATEGORY).orEmpty()
        val tile = requireArguments().getString(ARG_TILE)
        val adapter = SearchResultAdapter { product ->
            findNavController().navigate(R.id.productDetailFragment,
                Bundle().apply { putInt("productId", product.id) })
        }
        binding.products.layoutManager = LinearLayoutManager(requireContext())
        binding.products.adapter = adapter
        binding.sortButton.setOnClickListener { showSortOptions(adapter) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkStatus.observe(requireContext()).collect { online ->
                    if (!online && products.isEmpty()) binding.contentStatus.showOffline { load(slug, tile, adapter) }
                }
            }
        }
        load(slug, tile, adapter)
    }

    private fun load(slug: String, tile: String?, adapter: SearchResultAdapter) {
        loadJob?.cancel()
        if (products.isEmpty()) {
            if (NetworkStatus.isOnline(requireContext())) binding.contentStatus.showLoading()
            else binding.contentStatus.showOffline { load(slug, tile, adapter) }
            binding.sortButton.isVisible = false
            binding.resultCount.text = ""
        }
        if (!NetworkStatus.isOnline(requireContext())) return
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                products = withTimeout(15_000) {
                    CategoryProductFilter.filter(repository.getProductByCategory(slug), tile)
                }
                if (_binding == null) return@launch
                binding.sortButton.isVisible = products.isNotEmpty()
                if (products.isEmpty() && !NetworkStatus.isOnline(requireContext())) {
                    binding.contentStatus.showOffline {
                        load(slug, tile, adapter)
                    }
                } else if (products.isEmpty()) binding.contentStatus.showEmpty(
                    if (tile.isNullOrBlank()) "No products in this category yet"
                    else "No products for $tile yet"
                ) else binding.contentStatus.hide()
                renderProducts(adapter)
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                if (_binding == null) return@launch
                if (products.isNotEmpty()) {
                    binding.contentStatus.hide()
                    renderProducts(adapter)
                } else {
                    binding.sortButton.isVisible = false
                    if (NetworkStatus.isOnline(requireContext()))
                        binding.contentStatus.showError("Could not load products. Please retry.") { load(slug, tile, adapter) }
                    else binding.contentStatus.showOffline { load(slug, tile, adapter) }
                    adapter.submitItems(emptyList(), false)
                }
            }
        }
    }

    private fun renderProducts(adapter: SearchResultAdapter) {
        val sorted = when (sortOrder) {
            SearchSortOrder.PRICE_LOW -> products.sortedBy { it.price }
            SearchSortOrder.PRICE_HIGH -> products.sortedByDescending { it.price }
            SearchSortOrder.NAME -> products.sortedBy { it.title.lowercase() }
            SearchSortOrder.NEWEST -> products.sortedByDescending { it.newArrival }
            SearchSortOrder.POPULARITY -> products.sortedWith(
                compareByDescending<ProductResponse> { it.ratingCount }.thenByDescending { it.ratingAverage })
            SearchSortOrder.DISCOUNT -> products.sortedByDescending { it.discountPercentage }
            SearchSortOrder.RELEVANCE -> products
        }
        adapter.submitItems(sorted, false)
        binding.resultCount.text = resources.getQuantityString(R.plurals.category_product_count,
            sorted.size, sorted.size)
    }

    private fun showSortOptions(adapter: SearchResultAdapter) {
        val choices = listOf(
            SearchSortOrder.RELEVANCE to "Recommended",
            SearchSortOrder.NEWEST to "What's new",
            SearchSortOrder.POPULARITY to "Popularity",
            SearchSortOrder.PRICE_LOW to "Price - low to high",
            SearchSortOrder.PRICE_HIGH to "Price - high to low",
            SearchSortOrder.DISCOUNT to "Discount",
            SearchSortOrder.NAME to "Name - A to Z",
        )
        val sheet = SheetSearchSortBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        choices.forEach { (order, label) ->
            val row = layoutInflater.inflate(R.layout.item_search_sort_option,
                sheet.sortOptions, false) as TextView
            row.text = label
            if (order == sortOrder) {
                row.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                row.contentDescription = "$label, selected"
            }
            row.setOnClickListener {
                sortOrder = order
                renderProducts(adapter)
                dialog.dismiss()
            }
            sheet.sortOptions.addView(row)
        }
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        loadJob = null
        binding.products.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_CATEGORY = "categorySlug"
        const val ARG_TILE = "tileName"
    }
}
