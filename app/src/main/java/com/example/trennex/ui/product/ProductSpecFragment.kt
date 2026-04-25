package com.example.trennex.ui.product

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trennex.R
import com.example.trennex.data.model.ProductResponse
import com.example.trennex.databinding.FragmentProductSpecBinding
import com.example.trennex.ui.product.model.SpecDetailAdapter
import com.example.trennex.ui.product.model.SpecDetailItem
import com.example.trennex.viewmodel.product.ProductDetailViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ProductSpecFragment : Fragment(R.layout.fragment_product_spec) {


    private var _binding: FragmentProductSpecBinding? = null
    private val binding get() = _binding!!

    private val args: ProductSpecFragmentArgs by navArgs()
    private val viewModel: ProductDetailViewModel by viewModels()
    private lateinit var detailsAdapter: SpecDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProductSpecBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detailsAdapter = SpecDetailAdapter()
        binding.rvSpecDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSpecDetails.adapter = detailsAdapter

        viewModel.fetchProductDetail(args.productId)
        observeProduct()

    }

    private fun observeProduct(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.products.collect {product->
                    product?.let {
                        bindSections(it)
                    }
                }
            }
        }
    }

    private fun bindSections(product: ProductResponse){
        val sections = linkedMapOf<String, List<SpecDetailItem>>()
        sections["Product Specifications"] = buildSpecificationItems(product)
        val warrantyInfo =  product.warrantyInformation.trim()
        if(warrantyInfo.isNotEmpty()){
            sections["Warranty"] = listOf(
                SpecDetailItem("Warranty Details", warrantyInfo),
                SpecDetailItem("Return Policy", product.returnPolicy)
            )
        }
        val manufactureInfo = extractManufacturerInfo(product)
        if(manufactureInfo.isNotEmpty()){
            sections["Manufacture Info"] = manufactureInfo
        }

        val description = listOf(
            SpecDetailItem("Description",product.description)
        ).filter { it.value.isNotBlank() }
        if(description.isNotEmpty()){
            sections["Description"] = description
        }
        renderSectionChips(sections)
    }

    private fun renderSectionChips(sections: LinkedHashMap<String, List<SpecDetailItem>>){
        binding.chipGroupSections.removeAllViews()
        if(sections.isEmpty()){
            detailsAdapter.submitList(emptyList())
            return
        }
        sections.entries.forEachIndexed { index, section ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = section.key
                isCheckable = true
                isClickable = true
                setEnsureMinTouchTargetSize(false)
                styleSelectionChip(this)
            }
            binding.chipGroupSections.addView(chip)

            if(index == 0){
                chip.isChecked = true
                detailsAdapter.submitList(section.value)
            }
            chip.setOnCheckedChangeListener {_,isChecked ->
                if(isChecked){
                    detailsAdapter.submitList(section.value)
                }
            }
        }
    }

    private fun styleSelectionChip(chip: Chip){
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val defaultState = intArrayOf()
        val states = arrayOf(checkedState,defaultState)
        chip.chipBackgroundColor = ColorStateList(
            states,
            intArrayOf("#2962FF".toColorInt(), "#F2F2F2".toColorInt())
        )
        chip.setTextColor(
            ColorStateList(
                states,
                intArrayOf("#FFFFFF".toColorInt(), "#212121".toColorInt())
            )
        )
        chip.chipStrokeWidth = 1f
        chip.chipStrokeColor = ColorStateList.valueOf("#D8D8D8".toColorInt())
    }

    private fun buildSpecificationItems(product: ProductResponse): List<SpecDetailItem>{
        val dimensions = product.dimensions
        val dimensionValue = listOfNotNull(
            dimensions?.width?.let { "W: $it" },
            dimensions?.height?.let { "H: $it" },
            dimensions?.depth?.let { "D: $it" }
        ).joinToString(" | ")

        return listOf(
            SpecDetailItem("Title", product.title),
            SpecDetailItem("Brand", product.brand.orEmpty()),
            SpecDetailItem("Category", product.category.orEmpty()),
            SpecDetailItem("SKU", product.sku.orEmpty()),
            SpecDetailItem("Price", "₹${product.price}"),
            SpecDetailItem("Availability", product.availabilityStatus.orEmpty()),
            SpecDetailItem("Weight", product.weight?.let { "$it g" }.orEmpty()),
            SpecDetailItem("Dimensions", dimensionValue),
            SpecDetailItem("Tags", product.tags?.joinToString(", ").orEmpty()),
            SpecDetailItem("Warranty", product.warrantyInformation),
            SpecDetailItem("Shipping", product.shippingInformation),
            SpecDetailItem("Return Policy", product.returnPolicy)
        ).filter { it.value.isNotBlank() }

    }

    private fun extractManufacturerInfo(product: ProductResponse): List<SpecDetailItem>{
        val items = mutableListOf<SpecDetailItem>()
        if(!product.brand.isNullOrBlank()){
            items.add(SpecDetailItem("Brand", product.brand.orEmpty()))
        }
        if (!product.sku.isNullOrBlank()) {
            items.add(SpecDetailItem("SKU", product.sku.orEmpty()))
        }
        if (!product.availabilityStatus.isNullOrBlank()) {
            items.add(SpecDetailItem("Availability", product.availabilityStatus.orEmpty()))
        }
        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}