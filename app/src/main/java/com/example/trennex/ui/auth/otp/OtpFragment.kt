package com.example.trennex.ui.auth.otp

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import com.example.trennex.R
import com.example.trennex.databinding.FragmentLoginBinding
import com.example.trennex.databinding.FragmentOtpBinding
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trennex.viewmodel.auth.OtpViewModel

class OtpFragment : Fragment(R.layout.fragment_otp) {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtpViewModel by viewModels()
    private lateinit var otpBoxes: List<EditText>

    private lateinit var verificationId: String
    
    private lateinit var phone: String



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        otpBoxes = listOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4,binding.otp5,binding.otp6)
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
        val args = OtpFragmentArgs.fromBundle(requireArguments())
        phone = args.phone
        verificationId = args.verificationId

        binding.tvSubtitle.text = "Code has been sent to ${mask(phone)}"

        setupOtpUI()
        viewModel.setTimer()
        binding.btnVerify.setOnClickListener {
            viewModel.verifyOtp(verificationId,getOtp())
        }
        binding.tvResend.setOnClickListener {
            if(binding.tvResend.isEnabled){
                activity?.let {hostActivity->
                    clearOtpInputs()
                    viewModel.resendOtp(hostActivity,phone)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.state.collect { state->
                    when(state){
                        is OtpUiState.Idle ->{
                            binding.btnVerify.isEnabled = false
                            binding.otpProgressbar.visibility = View.GONE
                        }
                        is OtpUiState.Typing ->{
                            binding.btnVerify.isEnabled = false
                            binding.otpProgressbar.visibility = View.GONE
                        }
                        is OtpUiState.Ready ->{
                            binding.btnVerify.isEnabled = true
                            binding.otpProgressbar.visibility = View.GONE
                        }
                        is OtpUiState.Loading ->{
                            binding.btnVerify.isEnabled = false
                            binding.otpProgressbar.visibility = View.VISIBLE
                        }
                        is OtpUiState.Success ->{
                            binding.otpProgressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "OTP verified successfully", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
                        }
                        is OtpUiState.Error ->{
                            binding.otpProgressbar.visibility = View.GONE
                            binding.btnVerify.isEnabled = false
                            showOtpError(state.message)
                        }
                        is OtpUiState.Timer -> updateTimer(state)
                        is OtpUiState.CodeResent -> showCodeResent(state)
                    }
                }
            }

        }
    }

    private fun setupOtpUI() {
        otpBoxes.forEachIndexed { index, otpEditText ->
            otpEditText.keyListener = DigitsKeyListener.getInstance("0123456789")
            otpEditText.filters = arrayOf(InputFilter.LengthFilter(1))
            otpEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    otpEditText.setBackgroundResource(R.drawable.otp_active)
                }else if (otpEditText.text.isEmpty()) {
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                }
            }

            otpEditText.addTextChangedListener {
                if (binding.textinputErrorTxt.isVisible) {
                    resetErrorState()
                }
                val value = getOtp()
                if (!it.isNullOrEmpty()) {
                    otpEditText.setBackgroundResource(R.drawable.otp_filled)
                    if (index < otpBoxes.size - 1) otpBoxes[index + 1].requestFocus()
                } else {
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                }
                viewModel.onOtpChanged(value)
            }
            otpEditText.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL && otpEditText.text.isEmpty() && index > 0) {
                    otpBoxes[index - 1].text.clear()
                    otpBoxes[index - 1].requestFocus()
                }
                false
            }
        }
    }

    private fun getOtp(): String = otpBoxes.joinToString(""){it.text.toString()}

    private fun updateTimer(state: OtpUiState.Timer){
        val colorsRes = if(state.canResend) R.color.colorPrimary else R.color.textSecondary
        if(state.canResend){
            binding.timer.visibility = View.GONE
            binding.tvResend.isEnabled = true
        }else{
            binding.timer.visibility = View.VISIBLE
            binding.timer.text = "Resend in ${state.time}s"
            binding.tvResend.isEnabled = false
        }
        context?.let {
            binding.tvResend.setTextColor(ContextCompat.getColor(it,colorsRes))
        }
    }
    private fun showCodeResent(state: OtpUiState.CodeResent){
        verificationId = state.verificationId
        binding.otpProgressbar.visibility = View.GONE
        binding.textinputErrorTxt.visibility = View.GONE
        binding.btnVerify.isEnabled = false
        Toast.makeText(requireContext(), "OTP sent again", Toast.LENGTH_SHORT).show()
        viewModel.setTimer()
    }

    private fun clearOtpInputs(){
        otpBoxes.forEach { otpBox->
            otpBox.text.clear()
            otpBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            otpBox.setBackgroundResource(R.drawable.otp_default)
        }
        binding.otp1.requestFocus()
        binding.textinputErrorTxt.visibility = View.GONE
    }

    private fun resetErrorState() {
        binding.textinputErrorTxt.visibility = View.GONE
        otpBoxes.forEach {
            val color = ContextCompat.getColor(requireContext(), R.color.white)
            it.setTextColor(color)
            if (it.text.isNotEmpty()) {
                it.setBackgroundResource(R.drawable.otp_filled)
            } else {
                it.setBackgroundResource(R.drawable.otp_default)
            }
        }
    }

    private fun showOtpError(message:String) {
        otpBoxes.forEach {
            it.setBackgroundResource(R.drawable.otp_error)
            it.setTextColor(ContextCompat.getColor(requireContext(),R.color.colorError))
        }
        binding.textinputErrorTxt.visibility = View.VISIBLE
        binding.textinputErrorTxt.text = message
        binding.textinputErrorTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.colorError))
    }
    fun mask(phone: String): String{
        if(phone.length < 5) return phone
        return phone.take(3) + "XXXXXXX" + phone.takeLast(2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}