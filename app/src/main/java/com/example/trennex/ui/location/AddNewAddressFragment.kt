package com.example.trennex.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetDeliverToBinding
import com.example.trennex.databinding.FragmentAddNewAddressBinding
import com.example.trennex.databinding.SelectingLocationDialogBinding
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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AddNewAddressFragment : Fragment(R.layout.fragment_add_new_address), OnMapReadyCallback {
    private var _binding: FragmentAddNewAddressBinding? = null
    private val binding get() = _binding!!
    private var addAddressDialog: AlertDialog? = null
    private lateinit var googleMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var placesClient: PlacesClient

    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter

    private var autoCompleteSessionToken: AutocompleteSessionToken? = null

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var isSelectingPlace = false

    private val args by navArgs<AddNewAddressFragmentArgs>()

    private var searchedLat = 0.0
    private var searchedLng = 0.0
    private var searchedAddress = ""
    private var searchedPlaceName = ""

    private var openWithCurrentLocation = false
    private var shouldMoveToCurrentLocationWhenMapReady = false

    private var isMapInitialized = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {granted ->
        if (!isAdded) return@registerForActivityResult
        if (granted) {
            checkLocationServicesAndMove()
        }else{
            openWithCurrentLocation = false
            Toast.makeText(
                requireContext(),
                "Location permission is required to use current location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ){result ->
        if (!isAdded || !openWithCurrentLocation) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK || isLocationEnabled()){
            showMapAndMoveToCurrentLocation()
        }else{
            Toast.makeText(
                requireContext(),
                "Please turn on GPS to use current location",
                Toast.LENGTH_SHORT
            ).show()
            showTurnOnLocationDialog()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddNewAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val apiKey = getString(R.string.MAP_API_KEY).trim()
        if (apiKey.isBlank()) {
            Toast.makeText(requireContext(), "Places API key is missing", Toast.LENGTH_LONG).show()
            Log.e("PlacesDebug", "MAP_API_KEY is blank. Add a valid Places key in strings.xml")
        } else if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }
        placesClient = Places.createClient(requireContext())

        if(args.openedFromSearch) {
            binding.mainContentLayout.visibility = View.VISIBLE
            initializeMap()
        }else{
            showLocationDialog()
        }

        searchedLat = args.latitude.toDoubleOrNull() ?: 0.0
        searchedLng = args.longitude.toDoubleOrNull() ?: 0.0
        searchedAddress = args.address
        searchedPlaceName = args.placeName


        if (searchedPlaceName.isNotBlank()){
            binding.locationSearchInput.setText(searchedPlaceName)
            binding.locationSearchInput.setSelection(
                searchedPlaceName.length
            )
        }

        binding.useMyCurrLocation.setOnClickListener {
            checkLocationPermission()
        }

        searchSuggestionAdapter = SearchSuggestionAdapter { prediction ->
            fetchPlaceAndMoveMap(
                prediction.placeId
            )
        }

        binding.searchSuggestionsRv.apply {
            adapter = searchSuggestionAdapter

            layoutManager = LinearLayoutManager(requireContext())
        }


        binding.locationSearchInput.addTextChangedListener{
            val query = it.toString()

            if (isSelectingPlace) return@addTextChangedListener

            if (query.length > 2){
                autoCompleteSessionToken = AutocompleteSessionToken.newInstance()
                searchPlaces(query.trim())
            }else{
                binding.searchSuggestionsRv.visibility = View.GONE
            }
        }

        binding.addLocationBtn.setOnClickListener {
            showDeliverToBottomSheet()
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
            binding.mainContentLayout.alpha = 0f

            binding.mainContentLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            initializeMap()
        }
        dialogBinding.useCurrentLocationBtn.setOnClickListener {
            openWithCurrentLocation = true
            checkLocationPermission()
        }
        addAddressDialog?.show()

        addAddressDialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            setDimAmount(0.5f)

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        handleInitialLocation()

        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            animateCenterMarker()
            getAddressFromLatLng(
                center.latitude,
                center.longitude
            )
        }
    }

    @SuppressLint("NewApi")
    private fun getAddressFromLatLng(
        latitude: Double,
        longitude: Double
    ) {
       try {

           val geocoder = Geocoder(requireContext(), Locale.getDefault())
           geocoder.getFromLocation(
               latitude,
               longitude,
               1
           ) { addresses ->
               if (addresses.isNotEmpty()) {
                   val address = addresses[0]
                   val title = address?.subLocality
                       ?: address?.locality
                       ?: address?.featureName
                       ?: "Selected Location"
                   val fullAddress =
                       address?.getAddressLine(0)
                           ?: "Address not found"
                   requireActivity().runOnUiThread {
                       binding.addressTitleTv.text = title
                       binding.addressTv.text = fullAddress
                   }
               }
           }
       } catch (e: Exception){
           e.printStackTrace()
       }
    }
    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {

        if (!::googleMap.isInitialized) {
            shouldMoveToCurrentLocationWhenMapReady = true
            return
        }

        shouldMoveToCurrentLocationWhenMapReady = false

        binding.addressTitleTv.text =
            "Fetching location..."

        binding.addressTv.text =
            "Please wait while we get your current location"

        binding.useMyCurrLocation.isEnabled = false


        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener {location ->
            if (location != null){
                val latLng = LatLng(
                    location.latitude,
                    location.longitude
                )
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        17f
                    )
                )
                binding.useMyCurrLocation.isEnabled = true
            }else{
                binding.addressTitleTv.text =
                    "Location unavailable"

                binding.addressTv.text =
                    "Please try again"

                binding.useMyCurrLocation.isEnabled = true
            }
        }
    }

    private fun checkLocationPermission() {
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            checkLocationServicesAndMove()
        }else{
            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun checkLocationServicesAndMove(){
        if(isLocationEnabled()) {
            showMapAndMoveToCurrentLocation()
        }else{
            showTurnOnLocationDialog()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showMapAndMoveToCurrentLocation() {
        addAddressDialog?.dismiss()

        binding.mainContentLayout.visibility = View.VISIBLE
        binding.mainContentLayout.alpha = 0f

        binding.mainContentLayout.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        shouldMoveToCurrentLocationWhenMapReady = true
        openWithCurrentLocation = true

        initializeMap()
        moveToCurrentLocation()

    }

    private fun handleInitialLocation() {

        when  {
            searchedLat != 0.0 && searchedLng != 0.0 -> {
                moveToSearchedLocation()
            }

            openWithCurrentLocation || shouldMoveToCurrentLocationWhenMapReady -> {
                moveToCurrentLocation()
            }

            else -> {
                val defaultLocation = LatLng(20.5937, 78.9629)
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        defaultLocation,
                        5f
                    )
                )
            }
        }
    }

    private fun moveToSearchedLocation(){
        val latlng = LatLng(
            searchedLat,
            searchedLng
        )
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latlng,
                17f
            )
        )
        binding.addressTitleTv.text = searchedPlaceName
        binding.addressTv.text = searchedAddress
    }

    private fun showTurnOnLocationDialog(){
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()
        val builder =
            LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)

        val client =
            LocationServices.getSettingsClient(
                requireActivity()
            )

        val task =
            client.checkLocationSettings(
                builder.build()
            )

        task.addOnSuccessListener {
            if (!isAdded) return@addOnSuccessListener
            showMapAndMoveToCurrentLocation()
        }
        task.addOnFailureListener { exception ->

            if (exception is ResolvableApiException) {
                if (!isAdded) return@addOnFailureListener
                try {

                    val intentSenderRequest = IntentSenderRequest.Builder(
                        exception.resolution
                    ).build()

                    locationSettingsLauncher.launch(intentSenderRequest)

                } catch (e: IntentSender.SendIntentException) {
                    openWithCurrentLocation = false
                    e.printStackTrace()
                }
            }else{
                openWithCurrentLocation = false
                Toast.makeText(
                    requireContext(),
                    "Unable to open location settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun animateCenterMarker(){
        binding.centerMarker.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(120)
            .withEndAction {
                binding.centerMarker.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun initializeMap(){
        if (isMapInitialized) return
        isMapInitialized = true
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }



    private fun searchPlaces(
        query: String
    ){
        val request =
            FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(autoCompleteSessionToken)
                .build()

        placesClient.findAutocompletePredictions(
            request
        ).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions

            if (predictions.isNotEmpty()) {
                Log.d(
                    "PlacesDebug",
                    "Predictions size = ${predictions.size}"
                )
                binding.searchSuggestionsRv.visibility = View.VISIBLE
                searchSuggestionAdapter.submitList(
                    predictions
                )
            }else{
                binding.searchSuggestionsRv.visibility = View.GONE
            }
        }.addOnFailureListener { exception ->
            binding.searchSuggestionsRv.visibility = View.GONE
            Log.e("PlacesDebug", "Autocomplete failed: ${exception.message}", exception)
            Toast.makeText(requireContext(), "Unable to load place suggestions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPlaceAndMoveMap(
        placeId: String
    ){
        val fields = listOf(
            Place.Field.LAT_LNG,
            Place.Field.NAME,
            Place.Field.ADDRESS
        )

        val request = FetchPlaceRequest.newInstance(
            placeId,
            fields
        )

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->

                val place = response.place

                place.latLng?.let { latLng ->
                    googleMap.animateCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(
                                latLng,
                                17f
                            )
                    )
                }
                isSelectingPlace = true
                binding.searchSuggestionsRv
                    .visibility = View.GONE

                binding.locationSearchInput
                    .setText(place.name)

                binding.locationSearchInput
                    .setSelection(
                        place.name?.length ?: 0
                    )

                binding.locationSearchInput.clearFocus()

                isSelectingPlace = false

            }.addOnFailureListener {exception ->
                Log.e("PlacesDebug", "Fetch place failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Unable to fetch selected place", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showDeliverToBottomSheet(){
        val sheetBinding = BottomSheetDeliverToBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(sheetBinding.root)


        var selectedAddress = binding.addressTv.text.toString()
        sheetBinding.selectedAddressText.text = selectedAddress

        prefilledUserDetails(sheetBinding)

        sheetBinding.closeBtn.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        sheetBinding.editAddressBtn.setOnClickListener {
            sheetBinding.addressCard.visibility = View.GONE
            sheetBinding.editAddressInputLayout.visibility = View.VISIBLE
            sheetBinding.editAddressInput.setText(
                selectedAddress
            )
            sheetBinding.editAddressInput.setSelection(
                selectedAddress.length
            )
            sheetBinding.editAddressInput.requestFocus()
        }
        sheetBinding.editAddressInputLayout.setEndIconOnClickListener{
            val updatedAddress = sheetBinding.editAddressInput
                .text
                .toString()
                .trim()
            if (updatedAddress.isNotEmpty()){
                selectedAddress = updatedAddress
                sheetBinding.selectedAddressText.text = selectedAddress
                sheetBinding.addressCard.visibility = View.VISIBLE
                sheetBinding.editAddressInputLayout.visibility = View.GONE
            }
        }
        sheetBinding.saveAddressBtn.setOnClickListener {
            saveAddressToFirestore(sheetBinding, selectedAddress, bottomSheetDialog)
        }
        sheetBinding.flatInput.doAfterTextChanged { sheetBinding.flatInputLayout.error = null }
        sheetBinding.fullNameInput.doAfterTextChanged { sheetBinding.fullNameInputLayout.error = null }
        sheetBinding.mobileInput.doAfterTextChanged { sheetBinding.mobileInputLayout.error = null }
        bottomSheetDialog.show()
    }

    private fun prefilledUserDetails(sheetBinding: BottomSheetDeliverToBinding){
        if (!isAdded || FirebaseApp.getApps(requireContext()).isEmpty()) return
        val user = auth.currentUser ?: return
        val authPhone = user.phoneNumber.orEmpty().toEditablePhoneNumber()
        if (authPhone.isNotEmpty()) {
            sheetBinding.mobileInput.setText(authPhone)
        }

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val name = snapshot.getString(NAME_FIELD).orEmpty().trim()
                val phone = snapshot.getString(PHONE_FIELD).orEmpty().toEditablePhoneNumber()

                if (name.isNotEmpty() && sheetBinding.fullNameInput.text.isNullOrBlank()) {
                    sheetBinding.fullNameInput.setText(name)
                    sheetBinding.fullNameInput.setSelection(name.length)

                }
                val currentPhoneInput = sheetBinding.mobileInput.text?.toString().orEmpty()
                if (phone.isNotEmpty() && (currentPhoneInput.isBlank() || currentPhoneInput == authPhone)) {
                    sheetBinding.mobileInput.setText(phone)
                    sheetBinding.mobileInput.setSelection(phone.length)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AddAddress", "Could not load user details: ${exception.message}", exception)
            }

    }

    private fun String.toEditablePhoneNumber(): String {
        val digits = filter(Char::isDigit)
        return if (digits.length > PHONE_NUMBER_LENGTH) {
            digits.takeLast(PHONE_NUMBER_LENGTH)
        } else {
            digits
        }

    }
    private fun saveAddressToFirestore(
        sheetBinding: BottomSheetDeliverToBinding,
        selectedAddress: String,
        bottomSheetDialog: BottomSheetDialog
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please login before saving address", Toast.LENGTH_SHORT).show()
            return
        }
        val flatNo = sheetBinding.flatInput.text?.toString().orEmpty().trim()
        val fullName = sheetBinding.fullNameInput.text?.toString().orEmpty().trim()
        val mobile = sheetBinding.mobileInput.text?.toString().orEmpty().trim()
        val address = selectedAddress.trim()

        val addressType = if (sheetBinding.officeRadio.isChecked) {
            ADDRESS_TYPE_OFFICE
        } else {
            ADDRESS_TYPE_HOME
        }

        sheetBinding.flatInputLayout.error = null
        sheetBinding.fullNameInputLayout.error = null
        sheetBinding.mobileInputLayout.error = null

        when {
            flatNo.isBlank() -> {
                sheetBinding.flatInputLayout.error = "Enter flat, house, or building number"
                return
            }
            address.isBlank() || address == "Address not found" -> {
                Toast.makeText(requireContext(), "Please select a valid address", Toast.LENGTH_SHORT).show()
                return
            }
            fullName.isBlank() -> {
                sheetBinding.fullNameInputLayout.error = "Enter full name"
                return
            }
            mobile.length != PHONE_NUMBER_LENGTH -> {
                sheetBinding.mobileInputLayout.error = "Enter valid 10-digit mobile number"
                return
            }
        }

        sheetBinding.saveAddressBtn.isEnabled = false
        val addressData = hashMapOf(
            FLAT_NO_FIELD to flatNo,
            ADDRESS_FIELD to address,
            USER_NAME_FIELD to fullName,
            MOBILE_FIELD to mobile,
            ADDRESS_TYPE_FIELD to addressType,
            CREATED_AT_FIELD to FieldValue.serverTimestamp(),
            UPDATED_AT_FIELD to FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .collection(SAVED_ADDRESSES_COLLECTION)
            .add(addressData)
            .addOnSuccessListener {documentReference ->
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .update(
                        SELECTED_ADDRESS_ID_FIELD,
                        documentReference.id
                    )
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Address saved", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                        findNavController().popBackStack()
                    }

            }
            .addOnFailureListener { exception ->
                if (!isAdded) return@addOnFailureListener
                sheetBinding.saveAddressBtn.isEnabled = true
                Log.e("AddAddress", "Could not save address: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Failed to save address", Toast.LENGTH_SHORT).show()
            }

    }




    override fun onDestroyView() {
        super.onDestroyView()
        addAddressDialog?.dismiss()
        isMapInitialized = false
        shouldMoveToCurrentLocationWhenMapReady = false
        addAddressDialog = null
        _binding = null
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val NAME_FIELD = "name"
        const val PHONE_FIELD = "phone"

        const val SAVED_ADDRESSES_COLLECTION = "savedAddresses"
        const val SELECTED_ADDRESS_ID_FIELD = "selectedAddressId"
        const val FLAT_NO_FIELD = "flatNo"
        const val ADDRESS_FIELD = "address"
        const val USER_NAME_FIELD = "userName"
        const val MOBILE_FIELD = "mobile"
        const val ADDRESS_TYPE_FIELD = "addressType"
        const val CREATED_AT_FIELD = "createdAt"
        const val UPDATED_AT_FIELD = "updatedAt"
        const val ADDRESS_TYPE_HOME = "Home"
        const val ADDRESS_TYPE_OFFICE = "Office"
        const val PHONE_NUMBER_LENGTH = 10
    }
}