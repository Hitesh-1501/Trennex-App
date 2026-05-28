package com.example.trennex.ui.location

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
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

    private var isSelectingPlace = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {granted ->
        if (granted) {
            checkLocationServicesAndMove()
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

        showLocationDialog()
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
            addAddressDialog?.dismiss()
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
        val defaultLocation = LatLng(20.5937, 78.9629)
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation,
                5f
            )
        )

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
        val locationManager = requireContext().getSystemService(
            Context.LOCATION_SERVICE
        )as LocationManager

        val isEnabled = locationManager.isProviderEnabled(
            LocationManager.GPS_PROVIDER
        )
        if(isEnabled) {
            moveToCurrentLocation()
        }else{
            showTurnOnLocationDialog()
        }
    }

    private fun showTurnOnLocationDialog(){
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()
        val builder =
            LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val client =
            LocationServices.getSettingsClient(
                requireActivity()
            )

        val task =
            client.checkLocationSettings(
                builder.build()
            )

        task.addOnSuccessListener {

            moveToCurrentLocation()
        }
        task.addOnFailureListener { exception ->

            if (exception is ResolvableApiException) {

                try {

                    exception.startResolutionForResult(
                        requireActivity(),
                        1001
                    )

                } catch (e: IntentSender.SendIntentException) {

                    e.printStackTrace()
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        addAddressDialog?.dismiss()
        addAddressDialog = null
        _binding = null
    }
}