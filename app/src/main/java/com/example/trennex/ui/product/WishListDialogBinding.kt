package com.example.trennex.ui.product

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.trennex.R
import com.example.trennex.databinding.WishlistDialogBinding
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide

class WishListDialogBinding: DialogFragment() {

    interface WishlistActionListener {
        fun removeFromWishlist()
        fun goToWishlist()
    }
    var listener: WishlistActionListener? = null
    companion object{
        private const val ARG_X = "x"
        private const val ARG_Y = "y"
        private const val ARG_IMG_URL = "arg_image_url"
        fun newInstance(x: Int, y: Int,imageUrl: String?): WishListDialogBinding{
            val fragment = WishListDialogBinding()
            val bundle = Bundle().apply {
                putInt(ARG_X,x)
                putInt(ARG_Y,y)
                putString(ARG_IMG_URL,imageUrl)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
    private var _binding: WishlistDialogBinding? = null
    private val binding get() = _binding!!


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        _binding = WishlistDialogBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        binding.closeIv.setOnClickListener {
            dialog.dismiss()
        }

        binding.productCard.setOnClickListener {
            listener?.removeFromWishlist()
            dismiss()
        }
        binding.goToWishlist.setOnClickListener {
            listener?.goToWishlist()
            dismiss()
        }

        val imageUrl  = arguments?.getString(ARG_IMG_URL).orEmpty()
        if(imageUrl.isNotBlank()){
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(binding.imgProduct)
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val widthPx = (300 * resources.displayMetrics.density).toInt()
            setLayout(
                widthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val iconX = arguments?.getInt(ARG_X) ?: 0
            val iconY = arguments?.getInt(ARG_Y) ?: 0
            val params = attributes
            params.gravity = Gravity.TOP or Gravity.START
            params.x = iconX
            params.y = iconY
            attributes = params
            setWindowAnimations(R.style.PopupAnimation)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}