package com.example.trennex.ui.checkout

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetSelectLocationBinding
import com.example.trennex.databinding.FragmentCheckoutBinding
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.ui.cart.CartUiState
import com.example.trennex.ui.checkout.adapter.CheckoutItemsAdapter
import com.example.trennex.ui.home.adapters.SaveAddressAdapter
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.viewmodel.cart.CartViewModel
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
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()

    private val checkoutAdapter by lazy { CheckoutItemsAdapter() }

    private var bottomSheetBinding: BottomSheetSelectLocationBinding? = null
    private var locationBottomSheet: BottomSheetDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var saveAddressAdapter: SaveAddressAdapter

    private val locationPermissionRequester = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            promptEnableLocationServicesAndFetch()
        } else {
            Toast.makeText(requireContext(), "Location Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val placesSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            navigateToMapScreen(place)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCheckoutBinding.bind(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        (activity as? MainActivity)?.showToolBar(ToolBarType.CART, "Address")
        (activity as? MainActivity)?.toggleCartProgress(true)
        (activity as? MainActivity)?.updateCartStep(2)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvCheckoutItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = checkoutAdapter
        }
    }

    private fun setupListeners() {
        binding.tvChange.setOnClickListener {
            showLocationBottomSheet()
        }
        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_checkoutFragment_to_paymentFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: CartUiState) {
        val selectedItems = state.items.filter { it.isSelected }
        checkoutAdapter.submitList(selectedItems)

        state.selectedAddress?.let {
            binding.tvUserName.text = it.userName
            binding.tvFullAddress.text = it.displayAddress
            val phone = state.userPhone.ifBlank { it.mobile }
            binding.tvMobile.text = "Mobile : $phone"
        } ?: run {
            binding.tvUserName.text = "Select Delivery Address"
            binding.tvFullAddress.text = "Please select where to deliver your items"
            binding.tvMobile.text = if (state.userPhone.isNotBlank()) "Mobile : ${state.userPhone}" else ""
        }

        renderSaveAddresses(state.savedAddresses, state.selectedAddress)
    }

    private fun showLocationBottomSheet() {
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
            findNavController().navigate(R.id.action_checkoutFragment_to_addNewAddressFragment)
        }
        saveAddressAdapter = SaveAddressAdapter(
            onItemClick = { item -> selectSaveAddress(item) },
            onMoreClick = { anchor, item -> showAddressMenu(anchor, item) }
        )
        sheetBinding.savedAddressRv.adapter = saveAddressAdapter
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

    private fun openPlaceSearch() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(requireContext())
        placesSearchLauncher.launch(intent)
    }

    private fun navigateToMapScreen(place: Place) {
        val action = CheckoutFragmentDirections.actionCheckoutFragmentToAddNewAddressFragment(
            latitude = place.latLng?.latitude?.toString() ?: "",
            longitude = place.latLng?.longitude?.toString() ?: "",
            address = place.address ?: "",
            placeName = place.name ?: "",
            openedFromSearch = true
        )
        findNavController().navigate(action)
    }

    private fun renderSaveAddresses(addresses: List<AddressEntity>, selected: AddressEntity?) {
        val sheetBinding = bottomSheetBinding
        if (sheetBinding != null) {
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

    private fun showAddressMenu(anchor: View, item: SaveAddressAdapter.SavedAddressItem) {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.item_popup_menu, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        popupView.findViewById<View>(R.id.actionEdit).setOnClickListener {
            val action = CheckoutFragmentDirections.actionCheckoutFragmentToAddNewAddressFragment(
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
            popupWindow.dismiss()
            locationBottomSheet?.dismiss()
            findNavController().navigate(action)
        }

        popupView.findViewById<View>(R.id.actionDelete).setOnClickListener {
            val entity = viewModel.uiState.value.savedAddresses.find { it.id == item.id }
            if (entity != null) viewModel.deleteAddress(entity)
            popupWindow.dismiss()
        }
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 12f
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchor, -anchor.width / 2, 8)
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun requestLocationPermissionAndFetch() {
        if (hasLocationPermission()) {
            promptEnableLocationServicesAndFetch()
        } else {
            locationPermissionRequester.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun promptEnableLocationServicesAndFetch() {
        if (!isLocationServicesEnabled()) {
            showTurnOnLocationDialog()
            return
        }
        fetchCurrentAddress()
    }

    private fun showTurnOnLocationDialog() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { fetchCurrentAddress() }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(requireActivity(), 1001)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            }
        }
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentAddress() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(requireContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val address = result?.firstOrNull()?.getAddressLine(0) ?: "Lat:${location.latitude},Lng:${location.longitude}"
                viewModel.saveCurrentAddress(address, location.latitude, location.longitude)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to get address", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectSaveAddress(item: SaveAddressAdapter.SavedAddressItem) {
        val entity = viewModel.uiState.value.savedAddresses.find { it.id == item.id }
        if (entity != null) {
            viewModel.selectAddress(entity)
            locationBottomSheet?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
