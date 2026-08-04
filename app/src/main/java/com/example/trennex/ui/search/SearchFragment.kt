package com.example.trennex.ui.search

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.databinding.FragmentSearchBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.search.adapter.SearchOptionAdapter
import com.example.trennex.ui.search.adapter.SearchResultAdapter
import com.example.trennex.viewmodel.search.SearchViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search){
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchOptionAdapter: SearchOptionAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter

    private var searchInput: EditText? = null
    private var clearSearch: ImageView? = null
    private var wishlistIcon: ImageView? = null
    private var cartIcon: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initDao(AppDatabase.getInstance(requireContext()).recentSearchDao())
        setupAdapters()
        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        
        // Focus search input and show keyboard
        searchInput?.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun setupAdapters(){
        searchOptionAdapter = SearchOptionAdapter(
            onItemClick = {text, isRecommendation ->
                if(isRecommendation){
                    searchInput?.setText(text)
                    searchInput?.setSelection(text.length)
                }else{
                    performSearch(text)
                }
            },
            onRemoveClick = {text ->
                viewModel.deleteRecentSearch(text)
            }
        )
        searchResultAdapter = SearchResultAdapter{product ->
            if(findNavController().currentDestination?.id == R.id.searchFragment){
                val bundle = Bundle().apply {
                    putInt("productId",product.id)
                }
                findNavController().navigate(R.id.productDetailFragment,bundle)
            }
        }
    }
    private fun setupToolbar(){
        val mainActivity = (requireActivity() as MainActivity)
        searchInput = mainActivity.findViewById(R.id.searchInput)
        clearSearch = mainActivity.findViewById(R.id.clearSearch)
        wishlistIcon = mainActivity.findViewById(R.id.ivwishlist)
        cartIcon = mainActivity.findViewById(R.id.ivcart)

        wishlistIcon?.isVisible = false
        cartIcon?.isVisible = false
        clearSearch?.isVisible = false

        clearSearch?.setOnClickListener {
            searchInput?.text?.clear()
            clearSearch?.isVisible = false
        }

        wishlistIcon?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.searchFragment) {
                findNavController().navigate(R.id.wishlistFragment)
            }
        }

        cartIcon?.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.searchFragment) {
                findNavController().navigate(R.id.cartFragment)
            }
        }
    }
    private fun setupRecyclerViews() {
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = searchOptionAdapter
        binding.searchResultRecyclerView.adapter = searchResultAdapter
    }
    private fun setupListeners(){
        searchInput?.doAfterTextChanged { query ->
            val q = query?.toString()?.trim().orEmpty()
            clearSearch?.isVisible = q.isNotEmpty()

            when{
                q.isEmpty() -> {
                    clearSearch?.isVisible = false
                    wishlistIcon?.isVisible = false
                    cartIcon?.isVisible = false
                    viewModel.clearSearch()
                }
                q.length >= 1 ->{
                    wishlistIcon?.isVisible = false
                    cartIcon?.isVisible = false
                    viewModel.loadRecommendations(q)
                }
            }
        }
        searchInput?.setOnKeyListener { _,keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                performSearch(searchInput?.text?.toString()?.trim().orEmpty())
                true
            }else false
        }
    }

    private fun observeViewModel(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentSearches.collectLatest { searches ->
                val query = searchInput?.text?.toString()?.trim().orEmpty()
                if (query.isEmpty()) {
                    if (searches.isNotEmpty()) {
                        binding.listTitle.text = "RECENT SEARCHES"
                        binding.listTitle.isVisible = true
                        searchOptionAdapter.submitItems(searches, isRecommendation = false)
                    } else {
                        binding.listTitle.isVisible = false
                        searchOptionAdapter.submitItems(emptyList(), isRecommendation = false)
                    }
                    showSuggestions(hasQuery = false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.searchProgressBar.isVisible = state.isLoading
                
                when{
                    state.recommendations.isNotEmpty() && state.showRecommendations ->{
                        binding.listTitle.text = "RECOMMEND SEARCHES"
                        binding.listTitle.isVisible = true
                        searchOptionAdapter.submitItems(state.recommendations, isRecommendation = true)
                        showSuggestions(hasQuery = true)
                    }
                    state.hasResults ->{
                        val currentLM = binding.searchResultRecyclerView.layoutManager
                        val needsNewLM = if (state.isGridLayout) {
                            currentLM !is GridLayoutManager
                        } else {
                            currentLM !is LinearLayoutManager || currentLM is GridLayoutManager
                        }
                        
                        if (needsNewLM) {
                            binding.searchResultRecyclerView.layoutManager = if (state.isGridLayout) {
                                GridLayoutManager(requireContext(), 2)
                            } else {
                                LinearLayoutManager(requireContext())
                            }
                        }
                        
                        searchResultAdapter.submitItems(state.searchResults, state.isGridLayout)
                        binding.searchRecyclerView.isVisible = false
                        binding.listTitle.isVisible = false
                        binding.photoSearchSection.isVisible = false
                        binding.searchResultRecyclerView.isVisible = true
                        binding.noProductsContainer.isVisible = false
                        binding.bottomOptions.isVisible = true

                        binding.optionSecondary.isVisible = state.showGenderFilter
                        binding.optionDivider.isVisible = state.showGenderFilter

                        wishlistIcon?.isVisible = true
                        cartIcon?.isVisible = true
                        clearSearch?.isVisible = searchInput?.text?.toString()?.isNotEmpty() ?: false
                    }
                    state.error != null -> {
                        binding.searchRecyclerView.isVisible = false
                        binding.listTitle.isVisible = false
                        binding.bottomOptions.isVisible = false
                        binding.photoSearchSection.isVisible = false
                        binding.searchResultRecyclerView.isVisible = false
                        binding.noProductsContainer.isVisible = true
                        clearSearch?.isVisible = searchInput?.text?.toString()?.isNotEmpty() ?: false
                    }
                    !state.isLoading && !state.showRecommendations && !state.hasResults && searchInput?.text?.toString()?.trim()?.isEmpty() == true -> {
                        // Handled by recent searches observer
                    }
                }
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput?.windowToken, 0)

        if (searchInput?.text?.toString() != query) {
            searchInput?.setText(query)
            searchInput?.setSelection(query.length)
        }
        viewModel.searchProducts(query)
    }
    private fun showSuggestions(hasQuery: Boolean){
        binding.searchResultRecyclerView.isVisible = false
        binding.noProductsContainer.isVisible = false
        binding.searchRecyclerView.isVisible = true
        binding.photoSearchSection.isVisible = !hasQuery
        binding.bottomOptions.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchInput = null
        clearSearch = null
        wishlistIcon = null
        cartIcon = null
        _binding = null
    }
}