package com.example.trennex.ui.search

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.analytics.AnalyticsEvents
import com.example.trennex.analytics.AnalyticsNames
import com.example.trennex.analytics.AnalyticsParams
import com.example.trennex.ui.common.NetworkStatus
import kotlinx.coroutines.flow.combine
import com.example.trennex.data.local.cart.AppDatabase
import com.example.trennex.databinding.FragmentSearchBinding
import com.example.trennex.databinding.SheetSearchSortBinding
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.search.adapter.SearchOptionAdapter
import com.example.trennex.ui.search.adapter.SearchResultAdapter
import com.example.trennex.viewmodel.search.SearchViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class SearchFragment : Fragment(R.layout.fragment_search){
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchOptionAdapter: SearchOptionAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter

    private var searchInput: EditText? = null
    private var clearSearch: ImageView? = null
    private var micSearch: ImageView? = null
    private var suppressTextChanges = false
    private var lastTrackedSearch: String? = null
    private var nextSearchSource = "text"

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) processPhoto {
            withContext(Dispatchers.IO) { InputImage.fromFilePath(requireContext(), uri) }
        }
    }
    private val cameraCapture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) processPhoto { InputImage.fromBitmap(bitmap, 0) }
    }
    private val speechInput = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                    nextSearchSource = "voice"
                    performSearch(it)
                }
        }
    }

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
        setupRecyclerViews()
        setupSearchActions()
        observeViewModel()
        setupWindowInsets()
        binding.root.post {
            if (_binding == null || !isAdded) return@post
            setupToolbar()
            setupListeners()
            searchInput?.takeIf { viewModel.uiState.value.query.isEmpty() }?.let { input ->
                input.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomOptions) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }
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
            AppAnalytics.get().track(AnalyticsEvents.commerce(AnalyticsNames.SELECT_ITEM,
                listOf(AnalyticsEvents.item(product)),
                extra = mapOf(AnalyticsParams.ITEM_LIST_ID to "search_results")))
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
        micSearch = mainActivity.findViewById(R.id.micSearch)

        viewModel.uiState.value.query.takeIf { it.isNotEmpty() }?.let { query ->
            searchInput?.setText(query)
            searchInput?.setSelection(query.length)
        }

        clearSearch?.isVisible = !searchInput?.text.isNullOrEmpty()
        micSearch?.isVisible = searchInput?.text.isNullOrEmpty()

        clearSearch?.setOnClickListener {
            searchInput?.text?.clear()
            clearSearch?.isVisible = false
        }
        
        micSearch?.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search TrenNex products")
            }
            try {
                speechInput.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Voice search is unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupSearchActions() {
        binding.btnSearchWithCamera.setOnClickListener { cameraCapture.launch(null) }
        binding.btnSelectPhoto.setOnClickListener { photoPicker.launch("image/*") }
        binding.sortContainer.setOnClickListener {
            showSortSheet()
        }
        binding.optionSecondary.setOnClickListener {
            val options = arrayOf("All", "Men", "Women")
            val values = SearchGenderFilter.entries
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_TrenNex_SearchDialog)
                .setTitle("Filter by gender")
                .setSingleChoiceItems(options, values.indexOf(viewModel.uiState.value.genderFilter)) { dialog, index ->
                    viewModel.setGenderFilter(values[index])
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showSortSheet() {
        val sheet = SheetSearchSortBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        val choices = listOf(
            SearchSortOrder.NEWEST to "What's new",
            SearchSortOrder.PRICE_HIGH to "Price - high to low",
            SearchSortOrder.POPULARITY to "Popularity",
            SearchSortOrder.DISCOUNT to "Discount",
            SearchSortOrder.PRICE_LOW to "Price - low to high",
            SearchSortOrder.RELEVANCE to "Recommended",
            SearchSortOrder.NAME to "Name - A to Z",
        )
        choices.forEach { (order, label) ->
            val row = layoutInflater.inflate(R.layout.item_search_sort_option,
                sheet.sortOptions, false) as TextView
            row.text = label
            if (viewModel.uiState.value.sortOrder == order) {
                row.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                row.contentDescription = "$label, selected"
            }
            row.setOnClickListener {
                viewModel.setSortOrder(order)
                dialog.dismiss()
            }
            sheet.sortOptions.addView(row)
        }
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    private fun processPhoto(createImage: suspend () -> InputImage) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.searchProgressBar.isVisible = true
            binding.btnSearchWithCamera.isEnabled = false
            binding.btnSelectPhoto.isEnabled = false
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            try {
                val image = createImage()
                val text = recognizer.process(image).await().text
                val labels = labeler.process(image).await()
                    .filter { it.confidence >= 0.55f }.map { it.text }
                if (text.isBlank() && labels.isEmpty()) {
                    Toast.makeText(requireContext(), "No product clues found. Try a clearer photo", Toast.LENGTH_LONG).show()
                } else {
                    suppressTextChanges = true
                    searchInput?.setText("Photo search")
                    suppressTextChanges = false
                    clearSearch?.isVisible = true
                    micSearch?.isVisible = false
                    viewModel.searchByImageSignals(text, labels)
                    nextSearchSource = "photo"
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Toast.makeText(requireContext(), "Could not read the photo. Try another image", Toast.LENGTH_LONG).show()
            } finally {
                suppressTextChanges = false
                recognizer.close()
                labeler.close()
                _binding?.let {
                    it.searchProgressBar.isVisible = viewModel.uiState.value.isLoading
                    it.btnSearchWithCamera.isEnabled = true
                    it.btnSelectPhoto.isEnabled = true
                }
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
            if (suppressTextChanges) return@doAfterTextChanged
            val q = query?.toString()?.trim().orEmpty()
            clearSearch?.isVisible = q.isNotEmpty()
            micSearch?.isVisible = q.isEmpty()

            when{
                q.isEmpty() -> {
                    viewModel.clearSearch()
                    renderRecentSearches(viewModel.recentSearches.value)
                }
                q.length >= 1 ->{
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
                    renderRecentSearches(searches)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.uiState, NetworkStatus.observe(requireContext())) { state, online ->
                state to online
            }.collectLatest { (state, online) ->
                when {
                    state.searchResults.isNotEmpty() -> binding.contentStatus.hide()
                    !online && state.query.isNotBlank() -> binding.contentStatus.showOffline {
                        viewModel.searchProducts(state.query)
                    }
                    state.error == "ERROR LOADING PRODUCTS" -> binding.contentStatus.showError(
                        "Could not load products. Please retry."
                    ) { viewModel.searchProducts(state.query) }
                    else -> binding.contentStatus.hide()
                }
                binding.searchProgressBar.isVisible = state.isLoading
                
                when{
                    state.recommendations.isNotEmpty() && state.showRecommendations ->{
                        binding.listTitle.text = "RECOMMEND SEARCHES"
                        binding.listTitle.isVisible = true
                        searchOptionAdapter.submitItems(state.recommendations, isRecommendation = true)
                        showSuggestions(hasQuery = true)
                    }
                    state.hasResults ->{
                        if (!state.isLoading && lastTrackedSearch != state.query) {
                            lastTrackedSearch = state.query
                            AppAnalytics.get().track(AnalyticsEvents.search(
                                nextSearchSource, state.searchResults.size))
                            if (state.searchResults.isNotEmpty()) AppAnalytics.get().track(
                                AnalyticsEvents.commerce(AnalyticsNames.VIEW_ITEM_LIST,
                                    state.searchResults.take(20).map(AnalyticsEvents::item),
                                    extra = mapOf(AnalyticsParams.ITEM_LIST_ID to "search_results")))
                            nextSearchSource = "text"
                        }
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
                        
                        if (binding.searchRecyclerView.isVisible) {
                            binding.searchResultRecyclerView.alpha = 0f
                            binding.searchResultRecyclerView.isVisible = state.searchResults.isNotEmpty()
                            binding.searchResultRecyclerView.animate().alpha(1f).setDuration(300).start()
                            
                            binding.searchRecyclerView.animate().alpha(0f).setDuration(200).withEndAction {
                                binding.searchRecyclerView.isVisible = false
                                binding.searchRecyclerView.alpha = 1f
                            }.start()
                        } else {
                            binding.searchResultRecyclerView.isVisible = state.searchResults.isNotEmpty()
                            binding.searchRecyclerView.isVisible = false
                        }

                        binding.listTitle.isVisible = false
                        binding.photoSearchSection.isVisible = false
                        binding.noProductsContainer.isVisible = state.searchResults.isEmpty()
                        binding.noProductsMessage.text = "No products match this filter"
                        binding.bottomOptions.isVisible = true

                        binding.optionSecondary.isVisible = state.showGenderFilter
                        binding.optionDivider.isVisible = state.showGenderFilter
                        binding.optionSort.text = when (state.sortOrder) {
                            SearchSortOrder.RELEVANCE -> "SORT"
                            SearchSortOrder.NEWEST -> "NEW"
                            SearchSortOrder.PRICE_LOW -> "PRICE: LOW"
                            SearchSortOrder.PRICE_HIGH -> "PRICE: HIGH"
                            SearchSortOrder.POPULARITY -> "POPULAR"
                            SearchSortOrder.DISCOUNT -> "DISCOUNT"
                            SearchSortOrder.NAME -> "NAME"
                        }
                        binding.optionSecondary.text = when (state.genderFilter) {
                            SearchGenderFilter.ALL -> "GENDER"
                            SearchGenderFilter.MEN -> "MEN"
                            SearchGenderFilter.WOMEN -> "WOMEN"
                        }

                        clearSearch?.isVisible = searchInput?.text?.toString()?.isNotEmpty() ?: false
                    }
                    state.error != null -> {
                        binding.searchRecyclerView.isVisible = false
                        binding.listTitle.isVisible = false
                        binding.bottomOptions.isVisible = false
                        binding.photoSearchSection.isVisible = false
                        binding.searchResultRecyclerView.isVisible = false
                        binding.noProductsContainer.isVisible = true
                        val offline = !NetworkStatus.isOnline(requireContext())
                        binding.noProductsMessage.text = if (state.error == "ERROR LOADING PRODUCTS" || offline) {
                            if (!offline)
                                "Could not load products. Tap to retry."
                            else "No internet connection. Connect and tap to retry."
                        } else {
                            "No matching products found"
                        }
                        val canRetry = (state.error == "ERROR LOADING PRODUCTS" || offline) &&
                            state.query != "Photo search"
                        binding.noProductsContainer.isClickable = canRetry
                        binding.noProductsContainer.setOnClickListener(
                            if (canRetry) View.OnClickListener { viewModel.searchProducts(state.query) } else null
                        )
                        clearSearch?.isVisible = searchInput?.text?.toString()?.isNotEmpty() ?: false
                    }
                    !state.isLoading && !state.showRecommendations && !state.hasResults && searchInput?.text?.toString()?.trim()?.isEmpty() == true -> {
                        renderRecentSearches(viewModel.recentSearches.value)
                    }
                    !state.isLoading && !state.showRecommendations && !state.hasResults -> {
                        binding.listTitle.isVisible = false
                        searchOptionAdapter.submitItems(emptyList(), isRecommendation = true)
                        showSuggestions(hasQuery = true)
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
    private fun renderRecentSearches(searches: List<String>) {
        binding.listTitle.text = "RECENT SEARCHES"
        binding.listTitle.isVisible = searches.isNotEmpty()
        searchOptionAdapter.submitItems(searches, isRecommendation = false)
        showSuggestions(hasQuery = false)
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
        micSearch = null
        _binding = null
    }
}
