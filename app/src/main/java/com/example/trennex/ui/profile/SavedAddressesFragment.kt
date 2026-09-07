package com.example.trennex.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentSavedAddressesBinding
import com.example.trennex.repository.user.AddressEntity
import com.example.trennex.ui.profile.adapter.SavedAddressesAdapter
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.viewmodel.profile.SavedAddressesViewModel
import kotlinx.coroutines.launch

class SavedAddressesFragment : Fragment(R.layout.fragment_saved_addresses) {

    private var _binding: FragmentSavedAddressesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedAddressesViewModel by viewModels()
    private lateinit var addressesAdapter: SavedAddressesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedAddressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showToolBar(ToolBarType.TITLE, "Saved Addresses")

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        addressesAdapter = SavedAddressesAdapter(
            onEditClick = { address -> navigateToEditAddress(address) },
            onDeleteClick = { address -> showDeleteConfirmationDialog(address) }
        )
        binding.rvSavedAddresses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addressesAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddAddress.setOnClickListener {
            // Navigate to Add New Address screen (opens location dialog)
            val action = SavedAddressesFragmentDirections.actionSavedAddressesFragmentToAddNewAddressFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addresses.collect { list ->
                    addressesAdapter.submitList(list)
                    val isEmpty = list.isEmpty()
                    binding.rvSavedAddresses.isVisible = !isEmpty
                    binding.layoutEmptyAddresses.isVisible = isEmpty
                    binding.tvHeaderSaved.isVisible = !isEmpty
                }
            }
        }
    }

    private fun navigateToEditAddress(address: AddressEntity) {
        val action = SavedAddressesFragmentDirections.actionSavedAddressesFragmentToAddNewAddressFragment(
            latitude = address.latitude.toString(),
            longitude = address.longitude.toString(),
            address = address.address,
            placeName = address.placeName,
            openedFromSearch = true,
            isEditMode = true,
            addressId = address.id,
            flatNo = address.flatNo,
            mobile = address.mobile,
            addressType = address.addressType
        )
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmationDialog(address: AddressEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Address")
            .setMessage("Are you sure you want to delete this address?")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                deleteAddressWithLoading(address)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteAddressWithLoading(address: AddressEntity) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Deleting address...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        viewModel.deleteAddress(address) {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Address deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
