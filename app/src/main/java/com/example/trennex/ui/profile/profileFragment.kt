package com.example.trennex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentProfileBinding
import com.example.trennex.ui.profile.adapter.ProfileGridAdapter
import com.example.trennex.ui.profile.model.ProfileGridItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        updateUi()
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            updateUi()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogInSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        binding.AccountDetailsBox.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountDetailsFragment)
        }

        val gridItems = listOf(
            ProfileGridItem(R.drawable.ic_explore, "Explore Trenex"),
            ProfileGridItem(R.drawable.order_icon, "Orders"),
            ProfileGridItem(R.drawable.ic_help, "Help Center"),
            ProfileGridItem(R.drawable.ic_coupn, "Coupons")
        )

        val adapter = ProfileGridAdapter(gridItems)

        binding.profileGridRecycler.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.profileGridRecycler.adapter = adapter

    }


    private fun updateUi(){
        if(auth.currentUser != null){
            binding.profileCard.visibility = View.VISIBLE
            binding.nologincard.visibility = View.GONE
            binding.btnLogout.visibility = View.VISIBLE
            loadUserDetails()
        }else{
            binding.profileCard.visibility = View.GONE
            binding.nologincard.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.GONE
        }
    }

    private fun loadUserDetails(){
        val user = auth.currentUser ?: return
        binding.tvUserName.text = user.phoneNumber ?: "User"
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener {snapshot ->
                val name = snapshot.getString(NAME_FIELD).orEmpty().trim()
                if(name.isNotEmpty()){
                    binding.tvUserName.text = name
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Could not load profile details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUi(){
        binding.wishlistSection.apply {
            tvRowTitle.text = "Wishlist"
            tvRowSubtitle.text = "Your most loved styles"
            ivRowIcon.setImageResource(R.drawable.wishlist_items)
        }
        binding.notificationSection.apply {
            tvRowTitle.text = "Notifications"
            tvRowSubtitle.text = "Stay Updated, Instantly"
            ivRowIcon.setImageResource(R.drawable.ic_notification)
        }
        binding.saveCreditSection.apply {
            tvRowTitle.text = "Saved Credit / Debit & Gift Cards"
            tvRowSubtitle.text = "saved payment methods"
            ivRowIcon.setImageResource(R.drawable.ic_card)
        }
        binding.FAQsSection.apply {
            tvRowTitle.text = "FAQs"
            tvRowSubtitle.text = "Frequently Asked, Clearly Answered."
            ivRowIcon.setImageResource(R.drawable.ic_faq)
        }
        binding.termsSection.apply {
            tvRowTitle.text = "Terms,Policies and Licenses"
            tvRowSubtitle.text = "Clear Terms. Honest Policies"
            ivRowIcon.setImageResource(R.drawable.ic_terms)
        }
    }
    private companion object {
        const val USERS_COLLECTION = "users"
        const val NAME_FIELD = "name"
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}