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
        binding.progress.isVisible = true
        binding.message.isVisible = false
        binding.retry.isVisible = false
    }

    fun showEmpty(message: String) {
        isVisible = true
        binding.progress.isVisible = false
        binding.message.text = message
        binding.message.isVisible = true
        binding.retry.isVisible = false
    }

    fun showError(message: String, retry: () -> Unit) {
        isVisible = true
        binding.progress.isVisible = false
        binding.message.text = message
        binding.message.isVisible = true
        binding.retry.isVisible = true
        binding.retry.setOnClickListener { retry() }
    }

    fun hide() {
        isVisible = false
        binding.retry.setOnClickListener(null)
    }
}
