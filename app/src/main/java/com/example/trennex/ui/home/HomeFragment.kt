package com.example.trennex.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
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
import android.widget.PopupWindow
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
import com.example.trennex.viewmodel.product.ProductViewModel
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

    private var selectedAddressId: String? = null
    private var selectedAddressLoaded = false

    private lateinit var saveAddressAdapter: SaveAddressAdapter
    private var bottomSheetBinding: BottomSheetSelectLocationBinding? = null
    private var   locationBottomSheet: BottomSheetDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var shouldNavigateToAddress = false
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var savedAddressListener: ListenerRegistration? = null


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
            val place = Autocomplete.getPlaceFromIntent(
                result.data!!
            )
            navigateToMapScreen(place)
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        viewModel.fetchProducts()
        viewModel.fetchCategories()
        UserDetailsDialog.showIfNeeded(this){}
        fetchLoggedInUserName()
        fetchSavedAddresses()
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
        locationBottomSheet = BottomSheetDialog(requireContext())
        val sheet = locationBottomSheet!!
        val sheetBinding = BottomSheetSelectLocationBinding.inflate(layoutInflater)
        bottomSheetBinding = sheetBinding
        sheet.setContentView(sheetBinding.root)
        sheetBinding.closeBtn.setOnClickListener {
            shouldNavigateToAddress = false
            sheet.dismiss()
        }
        sheetBinding.currentLocationRow.setOnClickListener {
            shouldNavigateToAddress = false
            requestLocationPermissionAndFetch()
        }
        sheet.setOnDismissListener {
            if (shouldNavigateToAddress && isAdded){
                shouldNavigateToAddress = false
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.homeFragment){
                    navController.navigate(R.id.action_homeFragment_to_addNewAddressFragment)
                }
            }
        }
        sheetBinding.locationSearchInput.apply {
            isFocusable = false
            isCursorVisible = false
            keyListener = null

            setOnClickListener {
                openPlaceSearch()
            }
        }
        sheetBinding.addNewAddress.setOnClickListener {
            shouldNavigateToAddress = true
            sheet.dismiss()
        }
        saveAddressAdapter = SaveAddressAdapter(
           onItemClick = {item ->
             selectSaveAddress(item)
           },
           onMoreClick = {anchor, item -> showAddressMenu(anchor,item)}
        )
        sheetBinding.savedAddressRv.adapter =  saveAddressAdapter
        sheetBinding.savedAddressRv.layoutManager = LinearLayoutManager(requireContext())
        renderSaveAddresses()
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
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.FULLSCREEN,
            fields
        ).build(requireContext())

        placesSearchLauncher.launch(intent)
    }

    private fun navigateToMapScreen(
        place: Place
    ){
        val action = HomeFragmentDirections
            .actionHomeFragmentToAddNewAddressFragment(
                latitude = place.latLng?.latitude?.toString() ?: "",
                longitude = place.latLng?.longitude?.toString() ?: "",
                address = place.address ?: "",
                placeName = place.name ?: "",
                openedFromSearch = true

            )
        findNavController().navigate(action)
    }

    private fun renderSaveAddresses(){
        val sheetBinding = bottomSheetBinding
        if(sheetBinding != null){
            val hasSavedAddress = saveAddresses.isNotEmpty()
            sheetBinding.savedAddressRv.isVisible = hasSavedAddress
            sheetBinding.emptyAddressView.root.isVisible = !hasSavedAddress
        }
        if(saveAddresses.isNotEmpty()){
            val selectedItem = selectedAddressId?.let { id ->
                saveAddresses.find { it.id == id }
            }
            if (selectedItem != null){
                selectedAddress = selectedItem.displayAddress
                updateLocationBar()
            } else if (selectedAddressLoaded) {
                val firstAddress = saveAddresses.first()
                selectedAddressId = firstAddress.id
                selectedAddress = firstAddress.displayAddress
                updateSelectedAddressInFirestore(firstAddress.id)
                updateLocationBar()
            }
        } else if (selectedAddressLoaded) {
            selectedAddressId = null
            selectedAddress = null
            updateLocationBar()
        }
        if (this::saveAddressAdapter.isInitialized) {
            saveAddressAdapter.submitData(saveAddresses, selectedAddressId)
        }
    }
    private fun showAddressMenu(anchor: View, item: SaveAddressAdapter.SavedAddressItem){
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_popup_menu,null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<View>(R.id.actionEdit).setOnClickListener {
            val action = HomeFragmentDirections
                .actionHomeFragmentToAddNewAddressFragment(
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
            findNavController().navigate(action)
            true
        }
        popupView.findViewById<View>(R.id.actionDelete).setOnClickListener {
            deleteSaveAddress(item)
            popupWindow.dismiss()
        }
        popupWindow.elevation = 12f
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchor, -anchor.width / 2 , 8)
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
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            fetchCurrentAddress()
        }
        task.addOnFailureListener { exception ->
            if(exception is ResolvableApiException){
                try {

                    exception.startResolutionForResult(
                        requireActivity(),
                        1001
                    )
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
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if(location == null){
                Toast.makeText(
                    requireContext(),
                    "Unable to fetch current location",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val result  = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                val address = result?.firstOrNull()?.getAddressLine(0)
                    ?: "Lat:${location.latitude},Lng:${location.longitude}"

                saveCurrentAddressToFirestore(address)
            }catch (e: Exception){
                Toast.makeText(
                    requireContext(),
                    "Failed to get address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                requireContext(),
                "Failed to get current location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun fetchSavedAddresses() {
        val uid = auth.currentUser?.uid ?: return
        savedAddressListener?.remove()
        savedAddressListener = firestore.collection("users")
            .document(uid)
            .collection(SAVED_ADDRESSES_COLLECTION)
            .orderBy(CREATED_AT_FIELD, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                 if (exception != null){
                     if (isAdded){
                         Toast.makeText(
                             requireContext(),
                             "Failed to fetch saved addresses",
                             Toast.LENGTH_SHORT
                         ).show()
                     }
                     return@addSnapshotListener
                 }

                saveAddresses.clear()

                snapshot?.documents?.forEach { documentSnapshot ->
                    val address = documentSnapshot.getString(ADDRESS_FIELD).orEmpty().trim()
                    if (address.isNotEmpty()){
                        saveAddresses.add(
                            SaveAddressAdapter.SavedAddressItem(
                                id = documentSnapshot.id,
                                userName = documentSnapshot.getString(USER_NAME_FIELD).orEmpty().ifBlank { loginUserName },
                                flatNo = documentSnapshot.getString(FLAT_NO_FIELD).orEmpty(),
                                address = address,
                                mobile = documentSnapshot.getString(MOBILE).orEmpty(),
                                latitude = documentSnapshot.getDouble(LATITUDE_FIELD) ?: 0.0,
                                longitude = documentSnapshot.getDouble(LONGITUDE_FIELD) ?: 0.0,
                                placeName = documentSnapshot.getString(PLACE_NAME_FIELD).orEmpty(),
                                addressType = documentSnapshot.getString(ADDRESS_TYPE_FIELD).orEmpty().ifBlank {
                                    SaveAddressAdapter.ADDRESS_TYPE_HOME
                                }
                            )
                        )
                    }
                }
                fetchSelectedAddress()
                if (this::saveAddressAdapter.isInitialized) {
                    saveAddressAdapter.submitData(saveAddresses, selectedAddressId)
                }
            }
    }

    private fun deleteSaveAddress(item: SaveAddressAdapter.SavedAddressItem) {
        val uid = auth.currentUser?.uid ?: return
        if (item.id.isBlank()) return
        firestore.collection("users")
            .document(uid)
            .collection(SAVED_ADDRESSES_COLLECTION)
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                if (item.id == selectedAddressId && saveAddresses.none { it.id != item.id }){
                    selectedAddressId = null
                    selectedAddress = null
                    clearSelectedAddressInFirestore()
                    updateLocationBar()
                    renderSaveAddresses()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to delete address", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveCurrentAddressToFirestore(address: String){
        val user = auth.currentUser
        if (user == null){
            Toast.makeText(requireContext(), "Please login before saving address", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanedAddress = address.trim()
        if (cleanedAddress.isBlank()) return

        val existingAddress = saveAddresses.firstOrNull{
            it.address.normalizeAddressKey() == cleanedAddress.normalizeAddressKey()
        }

        if (existingAddress != null){
            selectSaveAddress(existingAddress)
            return
        }

        val userDocument = firestore.collection("users").document(user.uid)
        userDocument.collection(SAVED_ADDRESSES_COLLECTION)
            .whereEqualTo(ADDRESS_FIELD,cleanedAddress)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val duplicateDocument = snapshot.documents.firstOrNull()
                if (duplicateDocument != null) {
                    val item = SaveAddressAdapter.SavedAddressItem(
                        id = duplicateDocument.id,
                        userName = duplicateDocument.getString(USER_NAME_FIELD).orEmpty().ifBlank { loginUserName },
                        flatNo = duplicateDocument.getString(FLAT_NO_FIELD).orEmpty(),
                        address = duplicateDocument.getString(ADDRESS_FIELD).orEmpty(),
                        addressType = duplicateDocument.getString(ADDRESS_TYPE_FIELD).orEmpty().ifBlank {
                            SaveAddressAdapter.ADDRESS_TYPE_HOME
                        }
                    )
                    selectSaveAddress(item)
                    return@addOnSuccessListener
                }

                val addressData = hashMapOf(
                    FLAT_NO_FIELD to "",
                    ADDRESS_FIELD to cleanedAddress,
                    USER_NAME_FIELD to loginUserName,
                    ADDRESS_TYPE_FIELD to SaveAddressAdapter.ADDRESS_TYPE_HOME,
                    CREATED_AT_FIELD to FieldValue.serverTimestamp(),
                    UPDATED_AT_FIELD to FieldValue.serverTimestamp()
                )

                userDocument.collection(SAVED_ADDRESSES_COLLECTION)
                    .add(addressData)
                    .addOnSuccessListener { documentReference ->
                        selectedAddress = cleanedAddress
                        selectedAddressId = documentReference.id
                        selectedAddressLoaded = true
                        updateSelectedAddressInFirestore(documentReference.id)
                        renderSaveAddresses()
                        locationBottomSheet?.dismiss()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AddressSelection", "Could not save current address: ${exception.message}", exception)
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed to save current location", Toast.LENGTH_SHORT).show()
                        }
                    }
            }.addOnFailureListener {exception ->
                Log.e("AddressSelection", "Could not check duplicate address: ${exception.message}", exception)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to save current location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun selectSaveAddress(item: SaveAddressAdapter.SavedAddressItem){
        if (item.id.isBlank()) return
        selectedAddress = item.displayAddress
        selectedAddressId = item.id
        selectedAddressLoaded = true
        updateSelectedAddressInFirestore(item.id)
        updateLocationBar()
        renderSaveAddresses()
        locationBottomSheet?.dismiss()
    }

    private fun String.normalizeAddressKey(): String = trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.getDefault())

    private fun fetchSelectedAddress() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                selectedAddressId = snapshot.getString(SELECTED_ADDRESS_ID_FIELD)
                selectedAddressLoaded = true
                renderSaveAddresses()
            }
            .addOnFailureListener {
                selectedAddressLoaded = true
                renderSaveAddresses()
            }
    }

    private fun updateSelectedAddressInFirestore(addressId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .set(mapOf(SELECTED_ADDRESS_ID_FIELD to addressId), SetOptions.merge())
    }

    private fun updateLocationBar() {
        val locationAddress = (activity as MainActivity).findViewById<TextView>(R.id.location_address)
        locationAddress.text = selectedAddress ?: "Your delivery address"
    }

    private fun clearSelectedAddressInFirestore() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .set(mapOf(SELECTED_ADDRESS_ID_FIELD to FieldValue.delete()), SetOptions.merge())
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
        shouldNavigateToAddress = false
        savedAddressListener?.remove()
        savedAddressListener = null
        locationBottomSheet?.dismiss()
        locationBottomSheet = null
        bottomSheetBinding = null
        _binding = null
    }

    private companion object {
        const val SAVED_ADDRESSES_COLLECTION = "savedAddresses"
        const val FLAT_NO_FIELD = "flatNo"
        const val ADDRESS_FIELD = "address"
        const val USER_NAME_FIELD = "userName"
        const val ADDRESS_TYPE_FIELD = "addressType"
        const val CREATED_AT_FIELD = "createdAt"
        const val UPDATED_AT_FIELD = "updatedAt"
        const val SELECTED_ADDRESS_ID_FIELD = "selectedAddressId"

        const val LATITUDE_FIELD = "latitude"

        const val LONGITUDE_FIELD = "longitude"

        const val PLACE_NAME_FIELD = "placeName"

        const val MOBILE = "mobile"

    }
}