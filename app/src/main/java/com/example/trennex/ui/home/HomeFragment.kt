package com.example.trennex.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.IntentSender
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.example.trennex.viewmodel.home.HomeViewModel
import com.example.trennex.repository.user.AddressEntity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get()  = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    
    private var bannerMediator: TabLayoutMediator? = null
    private var loginUserName = "Guest User"
    
    private lateinit var saveAddressAdapter: SaveAddressAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter
    private var bannerAdapter: HomeFragmentPagerAdapter? = null
    
    private var bottomSheetBinding: BottomSheetSelectLocationBinding? = null
    private var locationBottomSheet: BottomSheetDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

    private val placesSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            navigateToMapScreen(place)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        UserDetailsDialog.showIfNeeded(this){}
        
        initAdapters()
        setupLocationUi()
        observeViewModel()
    }

    private fun initAdapters() {
        categoryAdapter = CategoryAdapter(emptyList()) {
            viewModel.fetchProductsByCategory(it.slug)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        productAdapter = ProductAdapter(emptyList()) { product ->
            val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product.id)
            findNavController().navigate(action)
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            isNestedScrollingEnabled = false
            adapter = productAdapter
        }

        saveAddressAdapter = SaveAddressAdapter(
            onItemClick = { item -> selectSaveAddress(item) },
            onMoreClick = { anchor, item -> showAddressMenu(anchor, item) }
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.map { it.products }.distinctUntilChanged().collect {
                        productAdapter.updateData(it)
                    }
                }
                launch {
                    viewModel.uiState.map { it.categories }.distinctUntilChanged().collect {
                        categoryAdapter.updateData(it)
                    }
                }
                launch {
                    viewModel.uiState.map { it.banners }.distinctUntilChanged().collect {
                        setupBanners(it)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        loginUserName = state.userName
                        renderSaveAddresses(state.savedAddresses, state.selectedAddress)
                        
                        if (state.selectedAddress == null && !hasLocationPermission()) {
                            showLocationPermissionDialog()
                        }
                    }
                }
            }
        }
    }

    private fun setupLocationUi() {
        (activity as? MainActivity)?.findViewById<LinearLayout>(R.id.location_bar)?.setOnClickListener { 
            showLocationBottomSheet() 
        }
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
        locationBottomSheet = BottomSheetDialog(requireContext())
        val sheet = locationBottomSheet!!
        val sheetBinding = BottomSheetSelectLocationBinding.inflate(layoutInflater)
        bottomSheetBinding = sheetBinding
        sheet.setContentView(sheetBinding.root)
        sheetBinding.closeBtn.setOnClickListener {
            sheet.dismiss()
        }
        sheetBinding.currentLocationRow.setOnClickListener {
            requestLocationPermissionAndFetch()
        }
        sheetBinding.locationSearchInput.apply {
            isFocusable = false
            isCursorVisible = false
            keyListener = null
            setOnClickListener { openPlaceSearch() }
        }
        sheetBinding.addNewAddress.setOnClickListener {
            sheet.dismiss()
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.homeFragment){
                navController.navigate(R.id.action_homeFragment_to_addNewAddressFragment)
            }
        }
        sheetBinding.savedAddressRv.adapter =  saveAddressAdapter
        sheetBinding.savedAddressRv.layoutManager = LinearLayoutManager(requireContext())
        
        val state = viewModel.uiState.value
        renderSaveAddresses(state.savedAddresses, state.selectedAddress)
        
        sheet.show()
        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val targetHeight = (resources.displayMetrics.heightPixels * 0.82f).toInt()
            it.layoutParams.height = targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun openPlaceSearch(){
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(requireContext())
        placesSearchLauncher.launch(intent)
    }

    private fun navigateToMapScreen(place: Place){
        val action = HomeFragmentDirections.actionHomeFragmentToAddNewAddressFragment(
                latitude = place.latLng?.latitude?.toString() ?: "",
                longitude = place.latLng?.longitude?.toString() ?: "",
                address = place.address ?: "",
                placeName = place.name ?: "",
                openedFromSearch = true
            )
        findNavController().navigate(action)
    }

    private fun renderSaveAddresses(addresses: List<AddressEntity>, selected: AddressEntity?){
        val sheetBinding = bottomSheetBinding
        if(sheetBinding != null){
            val hasSavedAddress = addresses.isNotEmpty()
            sheetBinding.savedAddressRv.isVisible = hasSavedAddress
            sheetBinding.emptyAddressView.root.isVisible = !hasSavedAddress
        }

        if (this::saveAddressAdapter.isInitialized) {
            val items = addresses.map { 
                SaveAddressAdapter.SavedAddressItem(
                    id = it.id,
                    userName = it.userName,
                    flatNo = it.flatNo,
                    address = it.address,
                    mobile = it.mobile,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    placeName = it.placeName,
                    addressType = it.addressType
                )
            }
            saveAddressAdapter.submitData(items, selected?.id)
        }
    }

    private fun showAddressMenu(anchor: View, item: SaveAddressAdapter.SavedAddressItem){
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_address_item, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.actionEdit -> {
                    val action = HomeFragmentDirections.actionHomeFragmentToAddNewAddressFragment(
                        latitude = item.latitude.toString(),
                        longitude = item.longitude.toString(),
                        address = item.address,
                        placeName = item.placeName,
                        openedFromSearch = true,
                        isEditMode = true,
                        addressId = item.id,
                        flatNo = item.flatNo,
                        mobile = item.mobile,
                        addressType = item.addressType
                    )
                    locationBottomSheet?.dismiss()
                    findNavController().navigate(action)
                    true
                }
                R.id.actionDelete -> {
                    val entity = viewModel.uiState.value.savedAddresses.find { it.id == item.id }
                    if (entity != null) viewModel.deleteAddress(entity)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun requestLocationPermissionAndFetch(){
        if(hasLocationPermission()){
            promptEnableLocationServicesAndFetch()
        }else{
            locationPermissionRequester.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { fetchCurrentAddress() }
        task.addOnFailureListener { exception ->
            if(exception is ResolvableApiException){
                try {
                    exception.startResolutionForResult(requireActivity(), 1001)
                }catch (sendEx: IntentSender.SendIntentException){
                    sendEx.printStackTrace()
                }
            }
        }
    }

    private fun isLocationServicesEnabled(): Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentAddress(){
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if(location == null){
                Toast.makeText(requireContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val result  = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val address = result?.firstOrNull()?.getAddressLine(0) ?: "Lat:${location.latitude},Lng:${location.longitude}"
                viewModel.saveCurrentAddress(address)
            }catch (e: Exception){
                Toast.makeText(requireContext(), "Failed to get address", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectSaveAddress(item: SaveAddressAdapter.SavedAddressItem){
        val entity = viewModel.uiState.value.savedAddresses.find { it.id == item.id }
        if (entity != null) {
            viewModel.selectAddress(entity)
            locationBottomSheet?.dismiss()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBanners(images: List<String>) {
        if (images.isEmpty()) return
        
        val banner = images.take(5).mapIndexed { index, image -> BannerModel(index + 1 , image) }
        
        // Only update if banners changed
        if (bannerAdapter != null && images.size == bannerAdapter?.itemCount) {
             return
        }

        if(banner.size == 1){
            if (bannerAdapter == null || bannerAdapter?.itemCount != 1) {
                bannerAdapter = HomeFragmentPagerAdapter(this, banner)
                binding.rvBanners.adapter = bannerAdapter
                binding.bannerIndicator.removeAllTabs()
            }
            return
        }

        val bannerList = mutableListOf<BannerModel>()
        bannerList.add(banner.last())
        bannerList.addAll(banner)
        bannerList.add(banner.first())
        
        if (bannerAdapter == null || bannerAdapter?.itemCount != bannerList.size) {
            bannerAdapter = HomeFragmentPagerAdapter(this, bannerList)
            binding.rvBanners.adapter = bannerAdapter
            binding.rvBanners.setCurrentItem(1, false)
            
            bannerMediator?.detach()
            bannerMediator =  TabLayoutMediator(binding.bannerIndicator, binding.rvBanners) { tab, position ->
                if (position == 0 || position == bannerList.size - 1) {
                    tab.view.visibility = View.GONE
                }
            }
            bannerMediator?.attach()
            for (i in 0 until binding.bannerIndicator.tabCount) {
                val tab = binding.bannerIndicator.getTabAt(i)
                tab?.customView = layoutInflater.inflate(R.layout.banner_dot_tab, binding.bannerIndicator, false)
            }
            
            bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
            val callback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        val position = binding.rvBanners.currentItem
                        when (position) {
                            0 -> binding.rvBanners.setCurrentItem(bannerList.size - 2, false)
                            bannerList.size - 1 -> binding.rvBanners.setCurrentItem(1, false)
                        }
                    }
                }
            }
            bannerPageCallbacks = callback
            binding.rvBanners.registerOnPageChangeCallback(callback)
            binding.rvBanners.getChildAt(0).setOnTouchListener { _,event ->
                when(event.action){
                    MotionEvent.ACTION_DOWN -> bannerHandler.removeCallbacks(bannerRunnable)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bannerHandler.postDelayed(bannerRunnable,3000)
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerPageCallbacks?.let { binding.rvBanners.unregisterOnPageChangeCallback(it) }
        bannerPageCallbacks = null
        bannerMediator?.detach()
        bannerMediator = null
        locationBottomSheet?.dismiss()
        locationBottomSheet = null
        bottomSheetBinding = null
        _binding = null
    }
}
