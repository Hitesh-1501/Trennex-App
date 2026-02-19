package com.example.trennex.ui.product
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.trennex.databinding.LayoutAddToCartBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddToCartSheet: BottomSheetDialogFragment() {
    interface AddToCartActionListener {
        fun gotoCart()
    }
    var listener: AddToCartActionListener? = null

    private var _binding: LayoutAddToCartBinding? = null
    private val binding get() = _binding!!

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