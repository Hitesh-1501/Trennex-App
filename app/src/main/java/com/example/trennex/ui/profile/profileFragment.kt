package com.example.trennex.ui.profile

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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.trennex.R
import com.example.trennex.databinding.FragmentProfileBinding
import com.example.trennex.ui.profile.adapter.ProfileGridAdapter
import com.example.trennex.ui.profile.model.ProfileGridItem
import com.example.trennex.viewmodel.profile.ProfileUiState
import com.example.trennex.viewmodel.profile.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

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
        observeViewModel()

        binding.btnLogout.setOnClickListener {
            viewModel.signOut()
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

        val adapter = ProfileGridAdapter(gridItems) { item ->
            when (item.title) {
                "Orders" -> findNavController().navigate(R.id.action_profileFragment_to_ordersFragment)
                else -> Toast.makeText(requireContext(), "${item.title} clicked", Toast.LENGTH_SHORT).show()
            }
        }
        binding.profileGridRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.profileGridRecycler.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: ProfileUiState) {
        if (state.isLoggedIn) {
            binding.profileCard.visibility = View.VISIBLE
            binding.nologincard.visibility = View.GONE
            binding.btnLogout.visibility = View.VISIBLE
            binding.tvUserName.text = state.userName ?: state.phoneNumber ?: "User"
        } else {
            binding.profileCard.visibility = View.GONE
            binding.nologincard.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.GONE
        }
        
        state.error?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUi() {
        binding.wishlistSection.apply {
            tvRowTitle.text = "Wishlist"
            tvRowSubtitle.text = "Your most loved styles"
            ivRowIcon.setImageResource(R.drawable.wishlist_items)
            root.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_wishlistFragment)
            }
        }
        binding.notificationSection.apply {
            tvRowTitle.text = "Notifications"
            tvRowSubtitle.text = "Stay Updated, Instantly"
            ivRowIcon.setImageResource(R.drawable.ic_notification)
            root.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_notificationFragment)
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}