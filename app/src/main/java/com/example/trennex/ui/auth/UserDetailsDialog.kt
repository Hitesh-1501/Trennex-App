package com.example.trennex.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.trennex.databinding.DialogUserDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object UserDetailsDialog {
    private const val USER_COLLECTION = "users"
    private const val NAME_FIELD = "name"
    private const val EMAIL_FIELD = "email"
    private const val PHONE_FIELD = "phone"

    fun showIfNeeded(fragment: Fragment, phoneNumber: String? = null, onComplete: () -> Unit){
        if(!fragment.isAdded || FirebaseApp.getApps(fragment.requireContext()).isEmpty()){
            onComplete()
            return
        }
        val user  = FirebaseAuth.getInstance().currentUser
        if(user == null){
            onComplete()
            return
        }
        val userDocument = FirebaseFirestore.getInstance()
            .collection(USER_COLLECTION)
            .document(user.uid)
        userDocument.get()
            .addOnSuccessListener { snapshot ->
                val existingName = snapshot.getString(NAME_FIELD).orEmpty().trim()
                if(existingName.isNotEmpty()){
                    onComplete()
                }else{
                    showDetailsDialog(fragment,user.uid, phoneNumber?: user.phoneNumber.orEmpty(),onComplete)
                }
            }
            .addOnFailureListener {
                showDetailsDialog(fragment, user.uid, phoneNumber ?: user.phoneNumber.orEmpty(), onComplete)
            }
    }
    private fun showDetailsDialog(
        fragment: Fragment,
        uid: String,
        phoneNumber: String,
        onComplete: () -> Unit
    ){
        if(!fragment.isAdded) return
        val binding = DialogUserDetailsBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(binding.root)
            .setPositiveButton("Save",null)
            .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.etName.text?.toString().orEmpty().trim()
                val email = binding.etEmail.text?.toString().orEmpty().trim()
                if(name.isBlank()){
                    binding.nameInputLayout.error = "Name is required"
                    return@setOnClickListener
                }
                binding.nameInputLayout.error = null
                binding.saveProgress.visibility = View.VISIBLE
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                val userDetails =  mapOf(
                    NAME_FIELD to name,
                    EMAIL_FIELD to email,
                    PHONE_FIELD to phoneNumber
                )
                FirebaseFirestore.getInstance()
                    .collection(USER_COLLECTION)
                    .document(uid)
                    .set(userDetails, SetOptions.merge())
                    .addOnSuccessListener {
                        dialog.dismiss()
                        onComplete()
                    }
                    .addOnFailureListener { exception ->
                        binding.saveProgress.visibility = View.GONE
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        Toast.makeText(
                            fragment.requireContext(),
                            exception.localizedMessage ?: "Could not save details. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
        dialog.show()
    }
}