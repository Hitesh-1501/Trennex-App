package com.example.trennex.ui.auth.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentLoginBinding
import com.example.trennex.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get()  = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etPhone.addTextChangedListener { text ->
            viewModel.onPhoneChange(text.toString())
        }
        binding.btnGetOtp.setOnClickListener {
            viewModel.onGetOtpClick(binding.etPhone.text.toString())
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when(state){
                        is LoginUIState.Loading -> showLoading()
                        is LoginUIState.Success -> showSuccess()
                        is LoginUIState.Error -> showError(state.message)
                        is LoginUIState.Idle -> showIdle()
                        else ->  {
                            binding.phoneInputLayout.error = null
                            binding.btnGetOtp.isEnabled = true
                            binding.loginProgressbar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    fun showLoading(){
        binding.loginProgressbar.visibility = View.VISIBLE
        binding.btnGetOtp.isEnabled = false
        binding.root.animate().alpha(0.5f).setDuration(200).start()
    }
    fun showSuccess(){
        binding.root.alpha = 1.0f
        binding.loginProgressbar.visibility = View.GONE
        findNavController().navigate(R.id.action_loginFragment_to_otpFragment)
    }
    fun showError(message: String){
        binding.root.animate().alpha(1.0f).setDuration(200).start()
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = message
        binding.btnGetOtp.isEnabled = false
    }
    fun showIdle(){
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = null
        binding.btnGetOtp.isEnabled = false
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}