package com.example.trennex.ui.search

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.data.local.search.RecentSearchEntity
import com.example.trennex.databinding.FragmentSearchBinding
import com.example.trennex.repository.product.ProductRepository
import com.example.trennex.ui.search.adapter.SearchOptionAdapter
import com.example.trennex.ui.search.adapter.SearchResultAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search){
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private lateinit var searchOptionAdapter: SearchOptionAdapter
    private lateinit var resultAdapter: SearchResultAdapter

    private var searchInput: EditText? = null
    private var clearSearch: ImageView? = null

    private var wishlistIcon: ImageView? = null
    private var cartIcon: ImageView? = null
    private val recommendationCache = mutableListOf<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchOptionAdapter = SearchOptionAdapter {option -> searchInput?.setText(option)}
        resultAdapter = SearchResultAdapter()

        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = searchOptionAdapter
        binding.searchResultRecyclerView.adapter = resultAdapter


        val dao = AppDatabase.getInstance(requireContext()).recentSearchDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getRecentSearches().collectLatest {searches->
                if(searchInput?.text.isNullOrBlank()){
                    binding.listTitle.text = "RECENT SEARCHES"
                    searchOptionAdapter.submitItems(searches.map { it.query }, false)
                }
            }
        }

        val activeRoot = requireActivity()
        searchInput = activeRoot.findViewById(R.id.searchInput)
        clearSearch = activeRoot.findViewById(R.id.clearSearch)
        wishlistIcon = activeRoot.findViewById(R.id.ivwishlist)
        cartIcon = activeRoot.findViewById(R.id.ivcart)
        clearSearch?.setOnClickListener { searchInput?.text?.clear() }


        searchInput?.doAfterTextChanged {query->
            val q = query?.toString()?.trim().orEmpty()
            clearSearch?.isVisible = q.isNotEmpty()
            if(q.isEmpty()){
                wishlistIcon?.isVisible = true
                cartIcon?.isVisible = true
                showSuggestions(false)
            }else{
                wishlistIcon?.isVisible = false
                cartIcon?.isVisible = false
                loadRecommendation(q)
            }
        }
        searchInput?.setOnKeyListener { _,keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN){
                performSearch(searchInput?.text?.toString().orEmpty())
                true
            }else false
        }
    }

    private fun loadRecommendation(query: String){
     viewLifecycleOwner.lifecycleScope.launch {
         val items = repository.searchProducts(query).take(8).map { it.title }
         recommendationCache.clear()
         recommendationCache.addAll(items)
         binding.listTitle.text = "RECOMMENDED SEARCHES"
         searchOptionAdapter.submitItems(recommendationCache, isRecommendation = true)
         showSuggestions(true)
     }

    }


    private fun performSearch(query: String){
        if(query.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).recentSearchDao().insertRecentSearch(
                RecentSearchEntity(query = query))
            val results = repository.searchProducts(query)
            val isClothes = query.contains("cloth",true) || results.any{
                it.category?.contains("shirt", true) == true || it.category?.contains("dress", true) == true
            }
            binding.searchRecyclerView.layoutManager = if(isClothes){
                GridLayoutManager(requireContext(),2)
            }else{
                LinearLayoutManager(requireContext())
            }
            resultAdapter.submitItems(results,isClothes)
            binding.searchResultRecyclerView.isVisible = true
            binding.searchRecyclerView.isVisible = false
            binding.listTitle.isVisible = false
            binding.photoSearchSection.isVisible = false
            binding.bottomOptions.isVisible = true
            binding.optionSecondary.isVisible = isClothes
            binding.optionDivider.isVisible = isClothes
            wishlistIcon?.isVisible = true
            cartIcon?.isVisible = true
        }
    }

    private fun showSuggestions(hasQuery: Boolean) {
        binding.searchResultRecyclerView.isVisible = false
        binding.searchRecyclerView.isVisible = true
        binding.listTitle.isVisible = true
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