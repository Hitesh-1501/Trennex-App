package com.example.trennex.ui.location

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentAddNewAddressBinding
import com.example.trennex.databinding.SelectingLocationDialogBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class AddNewAddressFragment : Fragment(R.layout.fragment_add_new_address), OnMapReadyCallback {
    private var _binding: FragmentAddNewAddressBinding? = null
    private val binding get() = _binding!!
    private var addAddressDialog: AlertDialog? = null
    private lateinit var googleMap: GoogleMap
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddNewAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLocationDialog()
    }


    private fun showLocationDialog(){
        val dialogBinding = SelectingLocationDialogBinding.inflate(layoutInflater)
        addAddressDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialogBinding.farFromLocationBtn.setOnClickListener {
            addAddressDialog?.dismiss()
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val defaultLocation = LatLng(20.5937, 78.9629)
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation,
                5f
            )
        )
    }
    private fun initializeMap(){
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addAddressDialog?.dismiss()
        addAddressDialog = null
        _binding = null
    }
}