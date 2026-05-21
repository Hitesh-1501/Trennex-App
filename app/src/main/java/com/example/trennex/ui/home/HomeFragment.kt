package com.example.trennex.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetSelectLocationBinding
import com.example.trennex.databinding.DialogLocationPermissionBinding
import com.example.trennex.databinding.FragmentHomeBinding
import com.example.trennex.ui.auth.UserDetailsDialog
import com.example.trennex.ui.home.adapters.CategoryAdapter
import com.example.trennex.ui.home.adapters.HomeFragmentPagerAdapter
import com.example.trennex.ui.home.adapters.ProductAdapter
import com.example.trennex.ui.home.adapters.SaveAddressAdapter
import com.example.trennex.ui.home.model.BannerModel
import com.example.trennex.ui.home.model.CategoryModel
import com.example.trennex.ui.home.model.ProductModel
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.utils.wishlist.CollectionStore
import com.example.trennex.viewmodel.product.ProductViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get()  = _binding!!
    private val viewModel:  ProductViewModel by viewModels()
    private var bannerMediator: TabLayoutMediator? = null
    private val saveAddresses = mutableListOf<SaveAddressAdapter.SavedAddressItem>()
    private var loginUserName = "Guest User"
    private var selectedAddress: String? = null
    private lateinit var saveAddressAdapter: SaveAddressAdapter
    private var bottomSheetBinding: BottomSheetSelectLocationBinding? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val locationPermissionRequester = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions->
       val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
               permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if(granted){
            promptEnableLocationServicesAndFetch()
        }else{
            Toast.makeText(requireContext(),"Location Permission Denied",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerPageCallbacks: ViewPager2.OnPageChangeCallback? = null
    private val bannerRunnable = object : Runnable{
        override fun run() {
            val nextItem = binding.rvBanners.currentItem+1
            binding.rvBanners.setCurrentItem(nextItem,true)
            bannerHandler.postDelayed(this,3000)
        }

    }

    override fun onResume() {
        super.onResume()
        bannerHandler.postDelayed(bannerRunnable,3000)
    }

    override fun onPause() {
        super.onPause()
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchProducts()
        viewModel.fetchCategories()
        UserDetailsDialog.showIfNeeded(this){}
        fetchLoggedInUserName()
        setupLocationUi()
        if (selectedAddress == null && !hasLocationPermission()) {
            showLocationPermissionDialog()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    viewModel.products.collect { apiList ->
                        val list = apiList.map {
                            ProductModel(
                                id = it.id,
                                image = it.thumbnail,
                                name  = it.title,
                                price = it.price
                            )
                        }
                        setupProducts(list)
                        setupBanners(apiList.map{ it.thumbnail }.filter { it.isNotBlank() })
                    }
                }
                launch {
                    viewModel.categories.collect {categoryList ->
                        val categories = buildList {
                                add(CategoryModel(id = 0, title = "All",slug = null))
                                addAll(categoryList.mapIndexed { index , category->
                                    CategoryModel(
                                        id = index + 1,
                                        title = category.name,
                                        slug = category.slug
                                    )
                                })

                        }
                        setupCategories(categories)
                    }
                }
            }
        }

    }

    private fun setupLocationUi() {
        (activity as MainActivity).findViewById<LinearLayout>(R.id.location_bar).setOnClickListener { showLocationBottomSheet() }
        updateLocationBar()
    }
    private fun showLocationPermissionDialog(){
        val dialogBinding = DialogLocationPermissionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialogBinding.enableLocationBtn.setOnClickListener {
            dialog.dismiss()
            requestLocationPermissionAndFetch()
        }
        dialogBinding.selectLocationManuallyBtn.setOnClickListener {
            dialog.dismiss()
            showLocationBottomSheet()
        }
        dialog.show()
    }

    private fun showLocationBottomSheet(){
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetSelectLocationBinding.inflate(layoutInflater)
        bottomSheetBinding = sheetBinding
        sheet.setContentView(sheetBinding.root)
        sheetBinding.closeBtn.setOnClickListener { sheet.dismiss() }
        sheetBinding.currentLocationRow.setOnClickListener { requestLocationPermissionAndFetch() }
        saveAddressAdapter = SaveAddressAdapter(
           onItemClick = {item ->
               selectedAddress = item.address
               updateLocationBar()
               renderSaveAddresses()
               sheet.dismiss()
           },
           onMoreClick = {anchor, _ -> showAddressMenu(anchor)}
        )
        sheetBinding.savedAddressRv.adapter =  saveAddressAdapter
        sheetBinding.savedAddressRv.layoutManager = LinearLayoutManager(requireContext())
        renderSaveAddresses()
        sheet.show()
    }

    private fun renderSaveAddresses(){
        val sheetBinding = bottomSheetBinding
        if(sheetBinding != null){
            val hasSavedAddress = saveAddresses.isNotEmpty()
            sheetBinding.savedAddressRv.isVisible = hasSavedAddress
            sheetBinding.emptyAddressView.root.isVisible = !hasSavedAddress
        }
       if(selectedAddress.isNullOrBlank() && saveAddresses.isNotEmpty()){
           selectedAddress = saveAddresses.first().address
           updateLocationBar()
       }
        saveAddressAdapter.submitData(saveAddresses,selectedAddress,loginUserName)
    }
    private fun showAddressMenu(anchor: View){
        val menu = PopupMenu(requireContext(),anchor)
        menu.menu.add(0, 1, 0, "Edit")
        menu.menu.add(0, 2, 1, "Delete")
        menu.show()
    }
    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun requestLocationPermissionAndFetch(){
        if(hasLocationPermission()){
            promptEnableLocationServicesAndFetch()
            return
        }else{
            locationPermissionRequester.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    private fun promptEnableLocationServicesAndFetch(){
        if(!isLocationServicesEnabled()){
            showTurnOnLocationDialog()
            return
        }
        fetchCurrentAddress()
    }

    private fun showTurnOnLocationDialog(){
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Device location")
            .setMessage("Please turn on device location for accurate delivery address.")
            .setNegativeButton("No, thanks", null)
            .setPositiveButton("Turn On") { _, _ ->
                fetchCurrentAddress()
            }
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_drawable_white)

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
        dialog.findViewById<TextView>(com.google.android.material.R.id.alertTitle)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorSuccess)
        )
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.textPrimary)
        )
    }
    private fun isLocationServicesEnabled(): Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    @SuppressLint("MissingPermission")
    private fun fetchCurrentAddress(){
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location == null) {
            Toast.makeText(requireContext(), "Unable to find current location", Toast.LENGTH_SHORT).show()
            return
        }
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = result?.firstOrNull()?.getAddressLine(0) ?: "Lat:${location.latitude}, Lng:${location.longitude}"
        selectedAddress = address
        if(saveAddresses.none{it.address == address}){
            saveAddresses.add(0, SaveAddressAdapter.SavedAddressItem(loginUserName, address))
        }
        updateLocationBar()
        if(this::saveAddressAdapter.isInitialized) renderSaveAddresses()
    }

    private fun updateLocationBar() {
        val locationAddress = (activity as MainActivity).findViewById<TextView>(R.id.location_address)
        locationAddress.text = selectedAddress ?: "Your delivery address"
    }

    private fun fetchLoggedInUserName(){
        val uid = auth.currentUser?.uid?: return
        firestore.collection("users").document(uid).get().addOnSuccessListener { snapshot ->
            val value = snapshot.getString("name").orEmpty().trim()
            if(value.isNotEmpty()){
                loginUserName = value
                if(this::saveAddressAdapter.isInitialized) renderSaveAddresses()
            }
        }
    }
    private fun setupCategories(categories: List<CategoryModel>){
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            adapter = CategoryAdapter(categories){
                if(it.slug.isNullOrBlank()){
                    viewModel.fetchProducts()
                }else{
                    viewModel.fetchProductsByCategory(it.slug)
                }
            }
        }
    }

    private fun setupProducts(list: List<ProductModel>){
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            isNestedScrollingEnabled = false
            adapter = ProductAdapter(list) {product->
                val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product.id)
                findNavController().navigate(action)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanners(images: List<String>) {
        bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
        bannerMediator?.detach()

        if(images.isEmpty()){
            return
        }
        val banner = images.take(5).mapIndexed { index, image ->
            BannerModel(index + 1 , image)
        }

        if(banner.size == 1){
            val adapter = HomeFragmentPagerAdapter(this,banner)
            binding.rvBanners.adapter = adapter
            binding.bannerIndicator.removeAllTabs()
            return
        }

        val bannerList = mutableListOf<BannerModel>()
        bannerList.add(banner.last())
        bannerList.addAll(banner)
        bannerList.add(banner.first())
        val adapter = HomeFragmentPagerAdapter(this, bannerList)
        binding.rvBanners.adapter = adapter
        binding.rvBanners.setCurrentItem(1, false)
        bannerMediator =  TabLayoutMediator(binding.bannerIndicator, binding.rvBanners) { tab, position ->
            if (position == 0 || position == bannerList.size - 1) {
                tab.view.visibility = View.GONE
            }
        }
        bannerMediator?.attach()
        for (i in 0 until binding.bannerIndicator.tabCount) {
            val tab = binding.bannerIndicator.getTabAt(i)
            tab?.customView =
                layoutInflater.inflate(R.layout.banner_dot_tab, binding.bannerIndicator, false)
        }
        val callback = object : ViewPager2.OnPageChangeCallback() {

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        val position = binding.rvBanners.currentItem

                        when (position) {
                            0 -> binding.rvBanners.setCurrentItem(
                                bannerList.size - 2,
                                false
                            )

                            bannerList.size - 1 -> binding.rvBanners.setCurrentItem(
                                1,
                                false
                            )
                        }
                    }
                }
            }
        bannerPageCallbacks = callback
        binding.rvBanners.registerOnPageChangeCallback(callback)
        binding.rvBanners.getChildAt(0)
                .setOnTouchListener { _,event ->
                    when(event.action){
                        MotionEvent.ACTION_DOWN -> bannerHandler.removeCallbacks(bannerRunnable)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            bannerHandler.postDelayed(bannerRunnable,3000)
                    }
                    false
                }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
        bannerPageCallbacks = null
        bannerMediator?.detach()
        bannerMediator = null
        _binding = null
    }
}