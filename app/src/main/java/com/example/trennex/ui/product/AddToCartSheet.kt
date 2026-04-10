package com.example.trennex.ui.product
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.LayoutAddToCartBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddToCartSheet: BottomSheetDialogFragment() {
    interface AddToCartActionListener {
        fun gotoCart()
    }
    var listener: AddToCartActionListener? = null

    private var _binding: LayoutAddToCartBinding? = null
    private val binding get() = _binding!!


    companion object{
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val ARG_PRODUCT_TITLE = "arg_product_title"

        fun newInstance(imageUrl: String?, productTitle: String): AddToCartSheet {
           return AddToCartSheet().apply {
               arguments = Bundle().apply {
                   putString(ARG_IMAGE_URL,imageUrl)
                   putString(ARG_PRODUCT_TITLE,productTitle)
               }
           }
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutAddToCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        val imageUrl = arguments?.getString(ARG_IMAGE_URL).orEmpty()
        val productTitle = arguments?.getString(ARG_PRODUCT_TITLE).orEmpty()

        if(productTitle.isNotBlank()){
            binding.tvProductDetail.text = productTitle
        }

        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(binding.productImg)
        }

        binding.imgClose.setOnClickListener {
            dismiss()
        }
        binding.btnSkip.setOnClickListener {
            dismiss()
        }
        binding.btnGoToCart.setOnClickListener {
            listener?.gotoCart()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}