package com.example.trennex.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentSearchBinding
import com.example.trennex.ui.search.adapter.SearchOptionAdapter

class SearchFragment : Fragment(R.layout.fragment_search){
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val recentSearches = mutableListOf("Clothes", "laptops", "mobiles", "Ac")
    private val recommendations = listOf("Clothes for men", "Clothes for women", "Clothes for kids")
    private var searchInput: EditText? = null
    private var clearSearch: ImageView? = null
    private lateinit var adapter: SearchOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchOptionAdapter(
            onActionClick = {option->
                if(searchInput?.text.isNullOrBlank()){
                    recentSearches.remove(option)
                    adapter.submitItems(
                        items = recentSearches,
                        isRecommendation = false
                    )
                }else{
                    searchInput?.setText(option)
                    searchInput?.setSelection(option.length)
                }
            }
        )

        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = adapter
        adapter.submitItems(recentSearches, isRecommendation = false)

        val activeRoot = requireActivity()
        searchInput = activeRoot.findViewById(R.id.searchInput)
        clearSearch = activeRoot.findViewById(R.id.clearSearch)
        clearSearch?.setOnClickListener {
            searchInput?.text?.clear()

        }
        searchInput?.doAfterTextChanged {query->
            val hasQuery = !query.isNullOrBlank()
            clearSearch?.isVisible = hasQuery
            binding.photoSearchSection.isVisible  = !hasQuery
            binding.listTitle.text = if (hasQuery) "RECOMMENDED SEARCHES" else "RECENT SEARCHES"
            adapter.submitItems(if (hasQuery) recommendations else recentSearches, isRecommendation = hasQuery)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchInput = null
        clearSearch = null
        _binding = null
    }
    private fun EditText.doAfterTextChanged(action: (CharSequence?) -> Unit) {
        this.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                action(s)

            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
    }
}