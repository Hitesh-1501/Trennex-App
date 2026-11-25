package com.example.trennex.ui.auth

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.trennex.databinding.DialogUserDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.core.graphics.drawable.toDrawable

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
            .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {

            dialog.window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }

            var saveAttempt = 0
            binding.btnSave.setOnClickListener {
                val name = binding.etName.text?.toString().orEmpty().trim()
                val email = binding.etEmail.text?.toString().orEmpty().trim()
                if(name.isBlank()){
                    binding.nameInputLayout.error = "Name is required"
                    return@setOnClickListener
                }
                binding.nameInputLayout.error = null
                binding.saveProgress.visibility = View.VISIBLE
                binding.btnSave.isEnabled = false
                saveAttempt += 1
                val currentAttempt = saveAttempt
                val userDetails =  mapOf(
                    NAME_FIELD to name,
                    EMAIL_FIELD to email,
                    PHONE_FIELD to phoneNumber
                )
                val timeoutHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!dialog.isShowing || currentAttempt != saveAttempt) return@Runnable
                    binding.saveProgress.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        fragment.requireContext(),
                        "Saving is taking too long. Check your connection and try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val saveTask = FirebaseFirestore.getInstance()
                    .collection(USER_COLLECTION)
                    .document(uid)
                    .set(userDetails, SetOptions.merge())
                saveTask.addOnCompleteListener { result ->
                    if (currentAttempt != saveAttempt) return@addOnCompleteListener
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    if (result.isSuccessful) {
                        dialog.dismiss()
                        onComplete()
                    } else {
                        binding.saveProgress.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            fragment.requireContext(),
                            result.exception?.localizedMessage
                                ?: "Could not save details. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                timeoutHandler.postDelayed(timeoutRunnable, SAVE_TIMEOUT_MILLIS)
            }
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private const val SAVE_TIMEOUT_MILLIS = 15_000L
}
