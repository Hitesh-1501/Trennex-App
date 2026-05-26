package com.example.trennex.ui.location

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.example.trennex.databinding.FragmentAddNewAddressBinding
import com.example.trennex.databinding.SelectingLocationDialogBinding

class AddNewAddressFragment : Fragment() {
    private var _binding: FragmentAddNewAddressBinding? = null
    private val binding get() = _binding!!
    private var addAddressDialog: AlertDialog? = null
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

    override fun onDestroyView() {
        super.onDestroyView()
        addAddressDialog?.dismiss()
        addAddressDialog = null
        _binding = null
    }
}