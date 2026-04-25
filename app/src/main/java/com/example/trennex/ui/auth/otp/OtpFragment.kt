package com.example.trennex.ui.auth.otp

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import androidx.navigation.fragment.findNavController
import com.example.trennex.viewmodel.auth.OtpViewModel

class OtpFragment : Fragment(R.layout.fragment_otp) {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtpViewModel by viewModels()

    private lateinit var otpBoxes: List<EditText>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        otpBoxes = listOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4)
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
        val phone = args.phone

        val mask = mask(phone)
        binding.tvSubtitle.text = "code has been sent to +91$mask"

        setupOtpUI()
        viewModel.setTimer()
        binding.btnVerify.setOnClickListener {
            viewModel.verifyOtp(getOtp())
        }

        lifecycleScope.launch {
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
                    }
                    is OtpUiState.Loading ->{
                        binding.btnVerify.isEnabled = false
                        binding.otpProgressbar.visibility = View.VISIBLE
                    }
                    is OtpUiState.Success ->{
                        binding.otpProgressbar.visibility = View.GONE
                        findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
                    }
                    is OtpUiState.Error ->{
                        binding.otpProgressbar.visibility = View.GONE
                        binding.btnVerify.isEnabled = false
                        showOtpError(state.message)
                    }
                    is OtpUiState.Timer ->{
                        val colorsRes = if(state.canResend) R.color.colorPrimary else R.color.textSecondary
                        if(state.canResend){
                            binding.timer.visibility = View.GONE
                            binding.tvResend.isEnabled = true
                            context?.let {
                                binding.tvResend.setTextColor(ContextCompat.getColor(it,colorsRes))
                            }
                        }else{
                            binding.timer.visibility = View.VISIBLE
                            binding.timer.text = "Resend in ${state.time}s"
                            binding.tvResend.isEnabled = false
                            context?.let {
                                binding.tvResend.setTextColor(ContextCompat.getColor(it,colorsRes))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupOtpUI() {
        otpBoxes.forEachIndexed { index, otpEditText ->
            otpEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    otpEditText.setBackgroundResource(R.drawable.otp_active)
                }else if(otpEditText.text.isEmpty()){
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                }
            }

            otpEditText.addTextChangedListener {
                if(binding.textinputErrorTxt.isVisible){
                    resetErrorState()
                }
                val value = getOtp()
                if(!it.isNullOrEmpty()) {
                    otpEditText.setBackgroundResource(R.drawable.otp_filled)
                    if (index < otpBoxes.size - 1) otpBoxes[index + 1].requestFocus()
                }else {
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                }
                viewModel.onOtpChanged(value)
            }
            otpEditText.setOnKeyListener { _, keyCode, _ ->
                if(keyCode == KeyEvent.KEYCODE_DEL && otpEditText.text.isEmpty() && index > 0){
                    otpBoxes[index - 1].text.clear()
                    otpBoxes[index - 1].requestFocus()
                }
                false
            }
        }
    }

    private fun getOtp(): String =
        otpBoxes.joinToString(""){it.text.toString()}

    private fun resetErrorState(){
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
    private fun showOtpError(message:String){
        otpBoxes.forEach {
            it.setBackgroundResource(R.drawable.otp_error)
            it.setTextColor(ContextCompat.getColor(requireContext(),R.color.colorError))
        }
        binding.textinputErrorTxt.visibility = View.VISIBLE
        binding.textinputErrorTxt.text = message
        binding.textinputErrorTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.colorError))
    }
    fun mask(phone: String): String{
        if(phone.length < 3) return phone
        val visible = phone.take(3)
        return  visible + "XXXXXXX"
    }
}