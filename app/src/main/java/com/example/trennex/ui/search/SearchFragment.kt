package com.example.trennex.ui.search

import android.os.Bundle
import android.view.KeyEvent
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
import com.example.trennex.data.local.search.RecentSearchEntity
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
        setupAdapters()
        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        loadRecentSearches()
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
                viewLifecycleOwner.lifecycleScope.launch {
                    AppDatabase.getInstance(requireContext()).recentSearchDao()
                        .deleteRecentSearch(text)
                }
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
                    viewModel.clearRecommendations()
                    loadRecentSearches()
                    showSuggestions(hasQuery = false)
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
            viewModel.uiState.collectLatest { state ->
                when{
                    state.recommendations.isNotEmpty() && state.showRecommendations ->{
                        binding.listTitle.text = "RECOMMEND SEARCHES"
                        binding.listTitle.isVisible = true
                        searchOptionAdapter.submitItems(state.recommendations, isRecommendation = true)
                        showSuggestions(hasQuery = true)
                    }
                    state.hasResults ->{
                        binding.searchResultRecyclerView.layoutManager = if(state.isGridLayout){
                            GridLayoutManager(requireContext(),2)
                        }else{
                            LinearLayoutManager(requireContext())
                        }
                        searchResultAdapter.submitItems(state.searchResults,state.isGridLayout)
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
                        binding.listTitle.text = state.error
                        binding.listTitle.isVisible = true
                        binding.bottomOptions.isVisible = false
                        binding.photoSearchSection.isVisible = false
                        binding.searchResultRecyclerView.isVisible = false
                        binding.noProductsContainer.isVisible = true
                        clearSearch?.isVisible = searchInput?.text?.toString()?.isNotEmpty() ?: false
                    }

                }
            }
        }
    }

    private fun loadRecentSearches(){
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).recentSearchDao()
                .getRecentSearches().collectLatest {searches->
                    if(searches.isNotEmpty()){
                        binding.listTitle.text = "RECENT SEARCHES"
                        binding.listTitle.isVisible = true
                        searchOptionAdapter.submitItems(
                            searches.map { it.query },
                            isRecommendation = false
                        )
                    }else{
                        binding.listTitle.isVisible = false
                        searchOptionAdapter.submitItems(emptyList(),false)
                    }
                }
        }
    }
    private fun performSearch(query: String) {
        if (query.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (searchInput?.text?.toString() != query) {
                searchInput?.setText(query)
                searchInput?.setSelection(query.length)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                AppDatabase.getInstance(requireContext()).recentSearchDao()
                    .deleteRecentSearch(query)
                AppDatabase.getInstance(requireContext()).recentSearchDao()
                    .insertRecentSearch(RecentSearchEntity(query = query))
                viewModel.searchProducts(query)
            }
        }
    }
    private fun showSuggestions(hasQuery: Boolean){
        binding.searchResultRecyclerView.isVisible = false
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