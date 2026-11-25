package com.example.trennex.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentCategoryBinding
import com.example.trennex.ui.category.adapter.CategorySideBarAdapter
import com.example.trennex.ui.category.model.CategoryModel
import com.example.trennex.ui.category.subcategories.*
import com.example.trennex.viewmodel.category.CategoryViewModel
import kotlinx.coroutines.launch

class CategoryFragment : Fragment(R.layout.fragment_category) {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    private fun render(state: com.example.trennex.viewmodel.category.CategoryUiState) {
        binding.rvCategorySidebar.apply {
            if (adapter == null) {
                adapter = CategorySideBarAdapter(state.categories, state.selectedCategory?.id) {
                    viewModel.selectCategory(it)
                    updateSubCategoryFragment(it)
                }
                layoutManager = LinearLayoutManager(requireContext())
            }
        }

        state.selectedCategory?.let {
            binding.shopCategoryButton.text = "Shop ${it.name} products"
            binding.shopCategoryButton.setOnClickListener { _ ->
                openProducts(slugFor(it.name), it.name)
            }
            if (childFragmentManager.findFragmentById(R.id.categoryContentContainer) == null) {
                updateSubCategoryFragment(it)
            }
        }
    }

    private fun slugFor(name: String): String = when (name) {
        "Top Picks" -> "top-picks"
        "Toys,Baby,Books" -> "toys-books"
        else -> name.lowercase()
    }

    private fun updateSubCategoryFragment(category: CategoryModel) {
        val fragment = when (category.name) {
            "Top Picks" -> TopPicksFragment()
            "Fashion" -> FashionFragment()
            "Appliances" -> ApplianceFragment()
            "Mobiles" -> MobileFragment()
            "Electronics" -> ElectronicsFragment()
            "Home" -> HomeCategoryFragment()
            "Beauty" -> BeautyFragment()
            "Furniture" -> FurnitureFragment()
            "Toys,Baby,Books" -> ToysFragment()
            "Sports" -> SportsFragment()
            else -> TopPicksFragment()
        }
        openFragment(fragment)
    }

    fun openProducts(categorySlug: String, categoryTitle: String, tile: String? = null) {
        if (findNavController().currentDestination?.id != R.id.categoryFragment) return
        findNavController().navigate(R.id.categoryProductsFragment, Bundle().apply {
            putString(CategoryProductsFragment.ARG_CATEGORY, categorySlug)
            putString("categoryTitle", categoryTitle)
            putString(CategoryProductsFragment.ARG_TILE, tile.orEmpty())
        })
    }

    private fun openFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.categoryContentContainer, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
