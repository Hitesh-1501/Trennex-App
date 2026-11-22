package com.example.trennex.ui.category

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.databinding.FragmentCategoryProductsBinding
import com.example.trennex.databinding.SheetSearchSortBinding
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.search.SearchSortOrder
import com.example.trennex.ui.search.adapter.SearchResultAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class CategoryProductsFragment : Fragment(R.layout.fragment_category_products) {
    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private var products = emptyList<ProductResponse>()
    private var sortOrder = SearchSortOrder.RELEVANCE

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
        binding.retryButton.setOnClickListener { load(slug, tile, adapter) }
        load(slug, tile, adapter)
    }

    private fun load(slug: String, tile: String?, adapter: SearchResultAdapter) {
        binding.loading.isVisible = true
        binding.sortButton.isVisible = false
        binding.resultCount.text = ""
        binding.errorGroup.isVisible = false
        binding.emptyMessage.isVisible = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                products = CategoryProductFilter.filter(repository.getProductByCategory(slug), tile)
                if (_binding == null) return@launch
                binding.loading.isVisible = false
                binding.sortButton.isVisible = products.isNotEmpty()
                binding.emptyMessage.isVisible = products.isEmpty()
                binding.emptyMessage.text = if (tile.isNullOrBlank())
                    "No products in this category yet" else "No products for $tile yet"
                renderProducts(adapter)
            } catch (_: Exception) {
                if (_binding == null) return@launch
                binding.loading.isVisible = false
                binding.sortButton.isVisible = false
                binding.errorGroup.isVisible = true
                adapter.submitItems(emptyList(), false)
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
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_CATEGORY = "categorySlug"
        const val ARG_TILE = "tileName"
    }
}
