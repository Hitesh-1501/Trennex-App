package com.example.trennex.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.trennex.databinding.ViewContentStatusBinding

/** Shared full-content state for remote-backed screens. A populated screen can keep its content visible. */
class ContentStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding = ViewContentStatusBinding.inflate(LayoutInflater.from(context), this, true)

    init { isVisible = false }

    fun showLoading() {
        isVisible = true
        binding.offlineIllustration.isVisible = false
        binding.title.isVisible = false
        binding.progress.isVisible = true
        binding.message.isVisible = false
        binding.retry.isVisible = false
    }

    fun showEmpty(message: String) {
        isVisible = true
        binding.offlineIllustration.isVisible = false
        binding.title.isVisible = false
        binding.progress.isVisible = false
        binding.message.text = message
        binding.message.isVisible = true
        binding.retry.isVisible = false
    }

    fun showError(message: String, retry: () -> Unit) {
        isVisible = true
        binding.offlineIllustration.isVisible = false
        binding.title.isVisible = false
        binding.progress.isVisible = false
        binding.message.text = message
        binding.message.isVisible = true
        binding.retry.isVisible = true
        binding.retry.setOnClickListener { retry() }
    }

    fun showOffline(retry: () -> Unit) {
        isVisible = true
        binding.progress.isVisible = false
        binding.offlineIllustration.isVisible = true
        binding.title.text = "You're offline"
        binding.title.isVisible = true
        binding.message.text = "Connect to the internet to load this page."
        binding.message.isVisible = true
        binding.retry.isVisible = true
        binding.retry.setOnClickListener { retry() }
    }

    fun hide() {
        isVisible = false
        binding.retry.setOnClickListener(null)
    }
}
