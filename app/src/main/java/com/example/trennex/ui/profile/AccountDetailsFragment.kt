package com.example.trennex.ui.profile

import android.os.Bundle
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trennex.R
import com.example.trennex.databinding.BottomSheetVerifyPhoneBinding
import com.example.trennex.databinding.FragmentAccountDetailsBinding
import com.example.trennex.viewmodel.profile.AccountDetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class AccountDetailsFragment : Fragment() {
    private var _binding: FragmentAccountDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel : AccountDetailsViewModel by viewModels()
    private var verifyPhoneBinding: BottomSheetVerifyPhoneBinding? = null
    private var verifyPhoneDialog: BottomSheetDialog? = null
    private var otpBoxes: List<EditText> = emptyList()
    private var animatedVerifiedPhone: String? = null
    private var originalPhone: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
        viewModel.loadAccountDetails()
    }
    private fun setupListeners() {
        setPhoneEditable(false)
        setupMobileInputAction()
        binding.etMobile.addTextChangedListener{
            if(binding.etMobile.isFocusable){
                renderAccountState(viewModel.state.value)
            }
        }
        binding.btnSaveDetails.setOnClickListener {
            viewModel.saveDetails(
                binding.etName.text?.toString().orEmpty(),
                binding.etEmail.text?.toString().orEmpty(),
                binding.etMobile.text?.toString().orEmpty()
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderAccountState(state)
                    renderOtpState(state.otpState)
                    state.message?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        if(message == "Details updated successfully"){
                            parentFragmentManager.popBackStack()
                        }
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }
    private fun renderAccountState(state: AccountDetailsUiStat) {
        setTextIfNeeded(binding.etMobile, state.phone)
        setTextIfNeeded(binding.etName, state.name)
        setTextIfNeeded(binding.etEmail, state.email)
        setPhoneEditable(state.isPhoneEditable)
        if(originalPhone.isBlank() && state.phone.isNotBlank()){
            originalPhone = state.phone.filter(Char::isDigit)
        }
        binding.mobileInputLayout.isEndIconVisible = state.isPhoneVerified
        binding.mobileInputLayout.suffixText = if (state.isPhoneEditable) "Update" else "Change"
        binding.btnSaveDetails.isEnabled = !state.isSaving
        if (state.isPhoneEditable && !binding.etMobile.hasFocus()) {
            binding.etMobile.requestFocus()
            binding.etMobile.setSelection(binding.etMobile.text?.length ?: 0)
            showKeyboard(binding.etMobile)
        }
    }


    private fun renderOtpState(otpState: AccountOtpState){
        when(otpState) {
            AccountOtpState.Idle -> Unit
            is AccountOtpState.Sending ->{
                ensureVerifyPhoneSheet(otpState.phone)
                verifyPhoneBinding?.apply {
                    btnVerifyUpdate.isEnabled = false
                    tvOtpSubtitle.text = "Sending 6 digit OTP to ${maskPhone(otpState.phone)}"
                }
            }
            is AccountOtpState.CodeSent -> {
                ensureVerifyPhoneSheet(otpState.phone)
                resetOtpError()
                verifyPhoneBinding?.apply {
                    btnVerifyUpdate.isEnabled = true
                    tvOtpSubtitle.text = "Enter the 6 digit OTP sent to ${maskPhone(otpState.phone)}"
                }
            }
            is AccountOtpState.Ready -> {
                verifyPhoneBinding?.btnVerifyUpdate?.isEnabled = true
            }
            is AccountOtpState.Verifying -> {
                verifyPhoneBinding?.apply {
                    btnVerifyUpdate.isEnabled = false
                    tvOtpSubtitle.text = "Verifying OTP for ${maskPhone(otpState.phone)}"
                }
            }
            is AccountOtpState.Verified -> {
                if (animatedVerifiedPhone != otpState.phone) {
                    animatedVerifiedPhone = otpState.phone
                    playVerificationSuccess(otpState.phone)
                }
            }
            is AccountOtpState.Error -> {
                ensureVerifyPhoneSheet(otpState.phone)
                showOtpError(otpState.message)
            }
        }
    }

    private fun setTextIfNeeded(editText: EditText, value: String) {
        if (!editText.hasFocus() && editText.text?.toString() != value) {
            editText.setText(value)
        }
    }

    private fun setPhoneEditable(editable: Boolean) {
        binding.etMobile.isFocusable = editable
        binding.etMobile.isFocusableInTouchMode = editable
        binding.etMobile.isCursorVisible = editable
        binding.etMobile.isLongClickable = editable
    }

    private fun setupMobileInputAction(){
        binding.mobileInputLayout.setEndIconOnClickListener {
            if (binding.etMobile.isFocusable) {
                requestOtpForPhoneUpdate()
            }
        }
        binding.mobileInputLayout.post {
            val suffixTextView = binding.mobileInputLayout.findViewById<View>(com.google.android.material.R.id.textinput_suffix_text)
            suffixTextView?.setOnClickListener {
                if (binding.etMobile.isFocusable) {
                    requestOtpForPhoneUpdate()
                } else {
                    viewModel.enablePhoneEditing()
                }
            }
        }
    }
    private fun requestOtpForPhoneUpdate() {
        activity?.let { hostActivity ->
            viewModel.requestPhoneOtp(
                hostActivity,
                binding.etMobile.text?.toString().orEmpty()
            )
        }
    }

    private fun ensureVerifyPhoneSheet(phone: String){
        if(verifyPhoneDialog?.isShowing == true) return
        val sheetBinding = BottomSheetVerifyPhoneBinding.inflate(layoutInflater)
        verifyPhoneBinding = sheetBinding
        otpBoxes = listOf(
            sheetBinding.otp1,
            sheetBinding.otp2,
            sheetBinding.otp3,
            sheetBinding.otp4,
            sheetBinding.otp5,
            sheetBinding.otp6
        )
        setupOtpInputs()
        sheetBinding.tvOtpSubtitle.text = "Enter the 6 digit OTP sent to ${maskPhone(phone)}"
        sheetBinding.btnVerifyUpdate.setOnClickListener {
            val otp = getOtpCode()
            if(otp.length == OTP_LENGTH){
                viewModel.verifyPhoneOtp(otp)
            }else{
                showOtpError("Enter the $OTP_LENGTH digit OTP")
            }
        }
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)
        dialog.setOnDismissListener {
            verifyPhoneBinding = null
            verifyPhoneDialog =  null
            otpBoxes = emptyList()
            if(animatedVerifiedPhone == null){
                viewModel.resetOtpState()
            }
        }
        verifyPhoneDialog = dialog
        dialog.show()
        sheetBinding.otp1.post {
            sheetBinding.otp1.requestFocus()
            showKeyboard(sheetBinding.otp1)
        }
    }

    private fun setupOtpInputs(){
        otpBoxes.forEachIndexed { index, otpEditText ->
            otpEditText.keyListener = DigitsKeyListener.getInstance("0123456789")
            otpEditText.filters = arrayOf(InputFilter.LengthFilter(1))
            otpEditText.setBackgroundResource(R.drawable.otp_default)
            otpEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    otpEditText.setBackgroundResource(R.drawable.otp_active)
                } else if (otpEditText.text.isNullOrEmpty()) {
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                }
            }
            otpEditText.addTextChangedListener{
                resetOtpError()
                if(!it.isNullOrEmpty()){
                    otpEditText.setBackgroundResource(R.drawable.otp_filled)
                    otpEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    if(index < otpBoxes.lastIndex){
                        otpBoxes[index + 1].requestFocus()
                    }
                }else{
                    otpEditText.setBackgroundResource(R.drawable.otp_default)
                    otpEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
                val otp = getOtpCode()
                viewModel.onOtpChanged(otp)
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

    private fun getOtpCode(): String = otpBoxes.joinToString(separator = "") { it.text?.toString().orEmpty() }

    private fun resetOtpError() {
        otpBoxes.forEach { otpBox ->
            if (otpBox.text.isNullOrEmpty()) {
                otpBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                otpBox.setBackgroundResource(R.drawable.otp_default)
            } else {
                otpBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                otpBox.setBackgroundResource(R.drawable.otp_filled)
            }
        }
        verifyPhoneBinding?.tvOtpSubtitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
    }

    private fun showOtpError(message: String) {
        verifyPhoneBinding?.apply {
            btnVerifyUpdate.isEnabled = true
            tvOtpSubtitle.text = message
            tvOtpSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorError))
        }
        otpBoxes.forEach { otpBox ->
            otpBox.setBackgroundResource(R.drawable.otp_error)
            otpBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorError))
        }
    }

    private fun playVerificationSuccess(phone: String){
        val sheetBinding = verifyPhoneBinding ?: return
        sheetBinding.btnVerifyUpdate.isEnabled = false
        sheetBinding.otpContent.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                sheetBinding.otpContent.visibility = View.GONE
                sheetBinding.successContent.alpha = 0f
                sheetBinding.successContent.visibility = View.VISIBLE
                sheetBinding.successContent.animate().alpha(1f).setDuration(180).start()
                sheetBinding.successCircle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(360f)
                    .setDuration(650)
                    .withEndAction {
                        sheetBinding.root.postDelayed({
                            binding.etMobile.setText(phone)
                            binding.etMobile.clearFocus()
                            verifyPhoneDialog?.dismiss()
                            viewModel.resetOtpState()
                        }, 550)
                    }
            }
    }

    private fun showKeyboard(editText: EditText){
        val inputMethodManager = ContextCompat.getSystemService(requireContext(),
            InputMethodManager::class.java)
        inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun maskPhone(phone: String): String{
        return "$COUNTRY_CODE ${phone.take(2)}XXXXXX${phone.takeLast(2)}"
    }

    override fun onDestroyView() {
        verifyPhoneDialog?.dismiss()
        verifyPhoneBinding = null
        verifyPhoneDialog = null
        otpBoxes = emptyList()
        super.onDestroyView()
        _binding = null
    }
    private companion object {
        const val COUNTRY_CODE = "+91"
        const val OTP_LENGTH = 6
        const val PHONE_LENGTH = 10
    }
}