package com.example.trennex.ui.auth.login

import android.os.Bundle
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trennex.R
import com.example.trennex.databinding.FragmentLoginBinding
import com.example.trennex.ui.auth.UserDetailsDialog
import com.example.trennex.viewmodel.auth.LoginViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var handleAutoVerification = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = true
        }
        binding.etPhone.keyListener = DigitsKeyListener.getInstance("0123456789")
        binding.etPhone.filters = arrayOf(InputFilter.LengthFilter(PHONE_NUMBER_LENGTH))
        binding.etPhone.addTextChangedListener { text ->
            viewModel.onPhoneChange(text.toString())
        }
        binding.btnGetOtp.setOnClickListener {
            activity?.let { hostActivity->
                viewModel.sendOtp(hostActivity,binding.etPhone.text.toString())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginUIState.Loading -> showLoading()
                        is LoginUIState.CodeSent -> showCodeSent(state)
                        is LoginUIState.AutoVerified -> showAutoVerified()
                        is LoginUIState.Error -> showError(state.message)
                        is LoginUIState.Valid -> showValid()
                        is LoginUIState.Idle -> showIdle()
                    }
                }
            }
        }
    }
    private fun showLoading(){
        binding.loginProgressbar.visibility = View.VISIBLE
        binding.btnGetOtp.isEnabled = false
        binding.phoneInputLayout.error = null
        binding.root.animate().alpha(0.5f).setDuration(200).start()
    }
    fun showCodeSent(state: LoginUIState.CodeSent){
        binding.root.alpha = 1.0f
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = null
        viewModel.consumeNavigation()
        val actions = LoginFragmentDirections.actionLoginFragmentToOtpFragment(
            phone = state.phoneNumber,
            verificationId = state.verificationId
        )
        findNavController().navigate(actions)
    }
    private fun showAutoVerified(){
        binding.root.alpha = 1.0f
        binding.loginProgressbar.visibility = View.GONE
        if(handleAutoVerification) return
        handleAutoVerification = true
        Toast.makeText(requireContext(), "Phone verified successfully", Toast.LENGTH_SHORT).show()
        viewModel.consumeNavigation()
        UserDetailsDialog.showIfNeeded(this,null){
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun showError(message: String){
        binding.root.animate().alpha(1.0f).setDuration(200).start()
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = message
        binding.btnGetOtp.isEnabled = false
    }

    private fun showValid(){
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = null
        binding.btnGetOtp.isEnabled = true
        binding.root.alpha = 1.0f
    }
    private fun showIdle(){
        binding.loginProgressbar.visibility = View.GONE
        binding.phoneInputLayout.error = null
        binding.btnGetOtp.isEnabled = false
        binding.root.alpha = 1.0f
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private companion object {
        const val PHONE_NUMBER_LENGTH = 10
    }
}