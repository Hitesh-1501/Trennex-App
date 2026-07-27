package com.example.trennex.ui.auth.otp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.trennex.R
import com.example.trennex.databinding.FragmentOtpBinding
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trennex.ui.auth.UserDetailsDialog
import com.example.trennex.viewmodel.auth.OtpViewModel
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class OtpFragment : Fragment(R.layout.fragment_otp) {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtpViewModel by viewModels()
    private lateinit var otpBoxes: List<EditText>

    private lateinit var verificationId: String
    
    private lateinit var phone: String
    private var handledSuccess = false

    private var smsReceiverRegistered = false

    private val smsConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
            extractOtp(message)?.let { otp -> autofillOtp(otp, verifyAfterFill = true)  }
        }
    }

    private val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
            val extras = intent.extras ?: return
            val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return
            if (status.statusCode == CommonStatusCodes.SUCCESS) {
                val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
                val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                when {
                    message.isNotBlank() -> extractOtp(message)?.let { autofillOtp(it, verifyAfterFill = true) }
                    consentIntent != null -> smsConsentLauncher.launch(consentIntent)
                }
            }

        }
    }


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
        startSmsOtpListener()
        viewModel.setTimer()
        binding.btnVerify.setOnClickListener {
            viewModel.verifyOtp(verificationId,getOtp())
        }
        binding.tvResend.setOnClickListener {
            if(binding.tvResend.isEnabled){
                activity?.let {hostActivity->
                    clearOtpInputs()
                    startSmsOtpListener()
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
                            if(!handledSuccess){
                                handledSuccess = true
                                Toast.makeText(requireContext(), "OTP verified successfully", Toast.LENGTH_SHORT).show()
                                UserDetailsDialog.showIfNeeded(this@OtpFragment,phone){
                                    findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
                                }
                            }
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

    override fun onStart() {
        super.onStart()
        registerSmsReceiver()
    }

    override fun onStop() {
        unregisterSmsReceiver()
        super.onStop()
    }

    private fun setupOtpUI() {
        otpBoxes.forEachIndexed { index, otpEditText ->
            otpEditText.keyListener = DigitsKeyListener.getInstance("0123456789")
            otpEditText.filters = arrayOf(InputFilter.LengthFilter(1))
            otpEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
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

    private fun startSmsOtpListener() {
        SmsRetriever.getClient(requireActivity()).startSmsRetriever()
        SmsRetriever.getClient(requireActivity()).startSmsUserConsent(null)

    }

    private fun registerSmsReceiver(){
        if(smsReceiverRegistered) return

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            requireContext(),
            smsVerificationReceiver,
            filter,
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
        smsReceiverRegistered = true
    }

    private fun unregisterSmsReceiver() {
        if (!smsReceiverRegistered) return
        requireContext().unregisterReceiver(smsVerificationReceiver)
        smsReceiverRegistered = false
    }


    private fun autofillOtp(otp: String, verifyAfterFill: Boolean) {
        if (_binding == null || otp.length != OTP_LENGTH || handledSuccess) return

        binding.textinputErrorTxt.visibility = View.GONE
        otpBoxes.forEachIndexed { index, otpEditText ->
            otpEditText.setText(otp[index].toString())
            otpEditText.setSelection(otpEditText.text?.length ?: 0)
            otpEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            otpEditText.setBackgroundResource(R.drawable.otp_filled)
        }
        otpBoxes.last().clearFocus()
        viewModel.onOtpChanged(otp)
        if(verifyAfterFill){
            viewModel.verifyOtp(verificationId,otp)
        }
    }

    private fun extractOtp(message: String): String? {
        return OTP_REGEX.find(message)?.value
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
        startSmsOtpListener()
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

    private companion object {
        const val OTP_LENGTH = 6
        val OTP_REGEX = Regex("(?<!\\d)\\d{$OTP_LENGTH}(?!\\d)")
    }
}