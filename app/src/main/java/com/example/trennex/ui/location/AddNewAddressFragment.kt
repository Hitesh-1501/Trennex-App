package com.example.trennex.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetDeliverToBinding
import com.example.trennex.databinding.FragmentAddNewAddressBinding
import com.example.trennex.databinding.SelectingLocationDialogBinding
import com.example.trennex.ui.home.adapters.SaveAddressAdapter
import com.example.trennex.ui.location.adapter.SearchSuggestionAdapter
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Locale

class AddNewAddressFragment : Fragment(R.layout.fragment_add_new_address), OnMapReadyCallback {
    private var _binding: FragmentAddNewAddressBinding? = null
    private val binding get() = _binding!!
    private var addAddressDialog: AlertDialog? = null
    private var bottomSheetDialog: BottomSheetDialog? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter
    private var autoCompleteSessionToken: AutocompleteSessionToken? = null

    private val viewModel: LocationViewModel by viewModels()
    private val args by navArgs<AddNewAddressFragmentArgs>()

    private var searchedLat = 0.0
    private var searchedLng = 0.0
    private var searchedAddress = ""
    private var searchedPlaceName = ""
    private var openWithCurrentLocation = false
    private var shouldMoveToCurrentLocationWhenMapReady = false
    private var isMapInitialized = false
    private var isSelectingPlace = false
    private var skipNextAddresssLookup = false
    private var addressLookupRequestId = 0

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {granted ->
        if (granted) checkLocationServicesAndMove()
        else {
            openWithCurrentLocation = false
            Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK && openWithCurrentLocation){
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) checkLocationServicesAndMove()
            }, 1500)
        } else if (openWithCurrentLocation) {
            Toast.makeText(requireContext(), "GPS must be on", Toast.LENGTH_SHORT).show()
            showTurnOnLocationDialog()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddNewAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        placesClient = Places.createClient(requireContext())
        
        searchedLat = args.latitude.toDoubleOrNull() ?: 0.0
        searchedLng = args.longitude.toDoubleOrNull() ?: 0.0
        searchedAddress = args.address
        searchedPlaceName = args.placeName

        if (args.isEditMode || args.openedFromSearch) {
            binding.mainContentLayout.visibility = View.VISIBLE
            initializeMap()
        } else {
            showLocationDialog()
        }

        val initialText = if (searchedAddress.isNotBlank()) searchedAddress else searchedPlaceName
        if (initialText.isNotBlank()){
            binding.locationSearchInput.setText(initialText)
            binding.locationSearchInput.setSelection(initialText.length)
        }

        binding.useMyCurrLocation.setOnClickListener { checkLocationPermission() }

        searchSuggestionAdapter = SearchSuggestionAdapter { prediction ->
            fetchPlaceAndMoveMap(prediction.placeId)
        }

        binding.searchSuggestionsRv.apply {
            adapter = searchSuggestionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.locationSearchInput.doAfterTextChanged {
            val query = it.toString()
            if (isSelectingPlace) return@doAfterTextChanged
            if (query.length > 2){
                autoCompleteSessionToken = AutocompleteSessionToken.newInstance()
                searchPlaces(query.trim())
            }else{
                binding.searchSuggestionsRv.visibility = View.GONE
            }
        }
        
        binding.addLocationBtn.text = if(args.isEditMode) "Update Location" else "Add Location"
        binding.addLocationBtn.setOnClickListener { showDeliverToBottomSheet() }
        
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state.status) {
                        LocationUiState.Status.Success -> {
                            bottomSheetDialog?.dismiss()
                            Toast.makeText(requireContext(), if(args.isEditMode) "Address updated" else "Address saved", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.setFragmentResult("address_updated", Bundle())
                            findNavController().popBackStack()
                        }
                        LocationUiState.Status.Error -> {
                            Toast.makeText(requireContext(), state.error ?: "An error occurred", Toast.LENGTH_SHORT).show()
                            viewModel.resetStatus()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showLocationDialog(){
        val dialogBinding = SelectingLocationDialogBinding.inflate(layoutInflater)
        addAddressDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialogBinding.farFromLocationBtn.setOnClickListener {
            addAddressDialog?.dismiss()
            binding.mainContentLayout.visibility = View.VISIBLE
            initializeMap()
        }
        dialogBinding.useCurrentLocationBtn.setOnClickListener {
            openWithCurrentLocation = true
            checkLocationPermission()
        }
        addAddressDialog?.show()
        addAddressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        skipNextAddresssLookup = args.isEditMode && searchedAddress.isNotBlank()

        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            animateCenterMarker()
            if (skipNextAddresssLookup){
                skipNextAddresssLookup = false
                return@setOnCameraIdleListener
            }
            getAddressFromLatLng(center.latitude, center.longitude)
        }
        handleInitialLocation()
    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double) {
        val requestId = ++addressLookupRequestId
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val title = address.subLocality ?: address.locality ?: address.featureName ?: "Selected Location"
                    val fullAddress = address.getAddressLine(0) ?: "Address not found"
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isAdded && _binding != null && requestId == addressLookupRequestId) {
                            isSelectingPlace = true
                            binding.locationSearchInput.setText(fullAddress)
                            isSelectingPlace = false
                            binding.addressTitleTv.text = title
                            binding.addressTv.text = fullAddress
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        if (!::googleMap.isInitialized) {
            shouldMoveToCurrentLocationWhenMapReady = true
            return
        }
        shouldMoveToCurrentLocationWhenMapReady = false
        binding.addressTitleTv.text = "Fetching location..."
        binding.useMyCurrLocation.isEnabled = false

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (!isAdded || _binding == null) return@addOnSuccessListener
            if (location != null){
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                getAddressFromLatLng(location.latitude, location.longitude)
            }
            binding.useMyCurrLocation.isEnabled = true
        }.addOnFailureListener {
            binding.useMyCurrLocation.isEnabled = true
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            checkLocationServicesAndMove()
        }else{
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkLocationServicesAndMove(){
        if(isLocationEnabled()) showMapAndMoveToCurrentLocation()
        else showTurnOnLocationDialog()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showMapAndMoveToCurrentLocation() {
        addAddressDialog?.dismiss()
        binding.mainContentLayout.visibility = View.VISIBLE
        shouldMoveToCurrentLocationWhenMapReady = true
        initializeMap()
    }

    private fun handleInitialLocation() {
        when  {
            searchedLat != 0.0 && searchedLng != 0.0 -> moveToSearchedLocation()
            openWithCurrentLocation || shouldMoveToCurrentLocationWhenMapReady -> moveToCurrentLocation()
            else -> googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 5f))
        }
    }

    private fun moveToSearchedLocation(){
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(searchedLat, searchedLng), 17f))
        binding.addressTitleTv.text = searchedPlaceName.ifBlank { "Selected Location" }
        binding.addressTv.text = searchedAddress
        
        isSelectingPlace = true
        binding.locationSearchInput.setText(searchedAddress)
        isSelectingPlace = false
    }

    private fun showTurnOnLocationDialog(){
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
        LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(builder.build())
            .addOnSuccessListener { if (isAdded) showMapAndMoveToCurrentLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException && isAdded) {
                    try {
                        locationSettingsLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
                    } catch (e: IntentSender.SendIntentException) { e.printStackTrace() }
                }
            }
    }

    private fun animateCenterMarker(){
        binding.centerMarker.animate().scaleX(0.9f).scaleY(0.9f).setDuration(120).withEndAction {
            binding.centerMarker.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun initializeMap(){
        if (isMapInitialized) return
        isMapInitialized = true
        (childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment).getMapAsync(this)
    }

    private fun searchPlaces(query: String){
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(autoCompleteSessionToken)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            if (predictions.isNotEmpty()) {
                binding.searchSuggestionsRv.visibility = View.VISIBLE
                searchSuggestionAdapter.submitList(predictions)
            }else{
                binding.searchSuggestionsRv.visibility = View.GONE
            }
        }.addOnFailureListener { binding.searchSuggestionsRv.visibility = View.GONE }
    }

    private fun fetchPlaceAndMoveMap(placeId: String){
        val fields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.FORMATTED_ADDRESS)
        placesClient.fetchPlace(FetchPlaceRequest.newInstance(placeId, fields)).addOnSuccessListener { response ->
            val place = response.place
            place.latLng?.let { latLng -> googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f)) }
            isSelectingPlace = true
            binding.searchSuggestionsRv.visibility = View.GONE
            binding.locationSearchInput.setText(place.formattedAddress ?: place.name)
            binding.locationSearchInput.setSelection(binding.locationSearchInput.text?.length ?: 0)
            binding.locationSearchInput.clearFocus()
            isSelectingPlace = false
        }
    }

    private fun showDeliverToBottomSheet(){
        val sheetBinding = BottomSheetDeliverToBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog?.setContentView(sheetBinding.root)

        var currentSelectedAddress = binding.addressTv.text.toString()
        sheetBinding.selectedAddressText.text = currentSelectedAddress

        val userState = viewModel.uiState.value
        sheetBinding.fullNameInput.setText(userState.userName)
        sheetBinding.mobileInput.setText(userState.userPhone)

        if (args.isEditMode){
            sheetBinding.flatInput.setText(args.flatNo)
            sheetBinding.mobileInput.setText(args.mobile)
            if (args.addressType == SaveAddressAdapter.ADDRESS_TYPE_OFFICE) sheetBinding.officeRadio.isChecked = true
            else sheetBinding.homeRadio.isChecked = true
        }

        sheetBinding.closeBtn.setOnClickListener { bottomSheetDialog?.dismiss() }
        sheetBinding.editAddressBtn.setOnClickListener {
            sheetBinding.addressCard.visibility = View.GONE
            sheetBinding.editAddressInputLayout.visibility = View.VISIBLE
            sheetBinding.editAddressInput.setText(currentSelectedAddress)
            sheetBinding.editAddressInput.requestFocus()
        }
        sheetBinding.editAddressInputLayout.setEndIconOnClickListener{
            val updated = sheetBinding.editAddressInput.text.toString().trim()
            if (updated.isNotEmpty()){
                currentSelectedAddress = updated
                syncLocationUi(currentSelectedAddress)
                moveMapToAddress(currentSelectedAddress)
                sheetBinding.selectedAddressText.text = currentSelectedAddress
                sheetBinding.addressCard.visibility = View.VISIBLE
                sheetBinding.editAddressInputLayout.visibility = View.GONE
            }
        }
        sheetBinding.saveAddressBtn.text = if (args.isEditMode) "Update Address" else "Save Address"
        sheetBinding.saveAddressBtn.setOnClickListener {
            if (sheetBinding.editAddressInputLayout.visibility == View.VISIBLE) {
                val updated = sheetBinding.editAddressInput.text.toString().trim()
                if (updated.isNotEmpty()) {
                    currentSelectedAddress = updated
                    syncLocationUi(currentSelectedAddress)
                }
            }

            val flatNo = sheetBinding.flatInput.text.toString().trim()
            val fullName = sheetBinding.fullNameInput.text.toString().trim()
            val mobile = sheetBinding.mobileInput.text.toString().trim()
            val addressType = if (sheetBinding.officeRadio.isChecked) SaveAddressAdapter.ADDRESS_TYPE_OFFICE else SaveAddressAdapter.ADDRESS_TYPE_HOME

            if (flatNo.isBlank()) { sheetBinding.flatInputLayout.error = "Required"; return@setOnClickListener }
            if (fullName.isBlank()) { sheetBinding.fullNameInputLayout.error = "Required"; return@setOnClickListener }
            if (mobile.length != 10) { sheetBinding.mobileInputLayout.error = "Invalid"; return@setOnClickListener }

            val cameraTarget = googleMap.cameraPosition.target
            val data = mapOf(
                "flatNo" to flatNo,
                "address" to currentSelectedAddress,
                "userName" to fullName,
                "mobile" to mobile,
                "addressType" to addressType,
                "latitude" to cameraTarget.latitude,
                "longitude" to cameraTarget.longitude,
                "placeName" to binding.addressTitleTv.text.toString()
            )

            if (args.isEditMode) viewModel.updateAddress(args.addressId, data)
            else viewModel.saveAddress(data)
        }
        bottomSheetDialog?.show()
    }

    private fun syncLocationUi(address: String) {
        isSelectingPlace = true
        binding.locationSearchInput.setText(address)
        isSelectingPlace = false
        binding.addressTv.text = address
        binding.addressTitleTv.text = address.take(30) + if(address.length > 30) "..." else ""
    }

    private fun moveMapToAddress(addressStr: String) {
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressStr, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isAdded && ::googleMap.isInitialized) {
                            skipNextAddresssLookup = true
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addAddressDialog?.dismiss()
        bottomSheetDialog?.dismiss()
        isMapInitialized = false
        shouldMoveToCurrentLocationWhenMapReady = false
        addAddressDialog = null
        _binding = null
    }
}
