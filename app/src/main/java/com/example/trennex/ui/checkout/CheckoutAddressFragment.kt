package com.example.trennex.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetSelectLocationBinding
import com.example.trennex.databinding.FragmentCheckoutAddressBinding
import com.example.trennex.ui.checkout.adapter.CheckoutProductAdapter
import com.example.trennex.ui.home.adapters.SaveAddressAdapter
import com.example.trennex.ui.main.MainActivity
import com.example.trennex.ui.main.ToolBarType
import com.example.trennex.viewmodel.cart.CartViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class CheckoutAddressFragment : Fragment() {

    private var _binding: FragmentCheckoutAddressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()
    private val productAdapter = CheckoutProductAdapter()
    
    private var locationBottomSheet: BottomSheetDialog? = null
    private var bottomSheetBinding: BottomSheetSelectLocationBinding? = null
    private lateinit var saveAddressAdapter: SaveAddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showToolBar(ToolBarType.CHECKOUT, "Address")
        (activity as? MainActivity)?.updateCartStep(2)

        binding.rvCheckoutProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }

        binding.tvChangeAddress.setOnClickListener {
            showLocationBottomSheet()
        }

        binding.btnContinue.setOnClickListener {
            Toast.makeText(requireContext(), "Payment Section coming soon", Toast.LENGTH_SHORT).show()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val selectedItems = state.items.filter { it.isSelected }
                    if (selectedItems.isEmpty() && isAdded) {
                        findNavController().popBackStack()
                        return@collect
                    }
                    
                    productAdapter.submitList(selectedItems)
                    
                    state.selectedAddress?.let {
                        binding.tvUserName.text = it.userName
                        binding.tvFullAddress.text = it.displayAddress
                        binding.tvMobile.text = "Mobile : ${it.mobile}"
                    } ?: run {
                        binding.tvUserName.text = "No address selected"
                        binding.tvFullAddress.text = "Please select a delivery address"
                        binding.tvMobile.text = ""
                    }
                    
                    renderSaveAddresses(state.savedAddresses, state.selectedAddress)
                }
            }
        }
    }

    private fun showLocationBottomSheet() {
        locationBottomSheet = BottomSheetDialog(requireContext())
        val sheet = locationBottomSheet!!
        val sheetBinding = BottomSheetSelectLocationBinding.inflate(layoutInflater)
        bottomSheetBinding = sheetBinding
        sheet.setContentView(sheetBinding.root)
        
        sheetBinding.closeBtn.setOnClickListener { sheet.dismiss() }
        
        sheetBinding.addNewAddress.setOnClickListener {
            sheet.dismiss()
            findNavController().navigate(R.id.action_cartFragment_to_addNewAddressFragment)
        }
        
        saveAddressAdapter = SaveAddressAdapter(
            onItemClick = { item -> 
                viewModel.selectAddress(item.id)
                sheet.dismiss()
            },
            onMoreClick = { _, _ -> }
        )
        sheetBinding.savedAddressRv.adapter = saveAddressAdapter
        sheetBinding.savedAddressRv.layoutManager = LinearLayoutManager(requireContext())
        
        val state = viewModel.uiState.value
        renderSaveAddresses(state.savedAddresses, state.selectedAddress)
        
        sheet.show()
        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun renderSaveAddresses(addresses: List<com.example.trennex.repository.user.AddressEntity>, selected: com.example.trennex.repository.user.AddressEntity?) {
        val sheetBinding = bottomSheetBinding ?: return
        val hasSavedAddress = addresses.isNotEmpty()
        sheetBinding.savedAddressRv.visibility = if (hasSavedAddress) View.VISIBLE else View.GONE
        sheetBinding.emptyAddressView.root.visibility = if (hasSavedAddress) View.GONE else View.VISIBLE

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

    override fun onDestroyView() {
        super.onDestroyView()
        locationBottomSheet?.dismiss()
        _binding = null
    }
}