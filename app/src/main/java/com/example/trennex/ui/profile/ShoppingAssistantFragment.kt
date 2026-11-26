package com.example.trennex.ui.profile

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.analytics.AppAnalytics
import com.example.trennex.analytics.AnalyticsEvent
import com.example.trennex.analytics.AnalyticsNames
import com.example.trennex.analytics.AnalyticsParams
import com.example.trennex.ui.common.NetworkStatus
import com.example.trennex.databinding.FragmentShoppingAssistantBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class ShoppingAssistantFragment : Fragment(R.layout.fragment_shopping_assistant) {
    private var binding: FragmentShoppingAssistantBinding? = null
    private var sending = false
    private val conversation: ShoppingAssistantViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ui = FragmentShoppingAssistantBinding.bind(view)
        binding = ui
        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { root, insets ->
            root.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
            insets
        }
        if (conversation.entries.isEmpty()) {
            addMessage("Tell me what you are looking for. I can suggest products from the current catalog.", false)
        } else {
            conversation.entries.forEach { entry ->
                when (entry) {
                    is AssistantEntry.Message -> renderMessage(entry)
                    is AssistantEntry.Product -> renderProduct(entry)
                }
            }
        }
        ui.assistantSend.setOnClickListener { sendQuestion() }
        ui.assistantQuestion.setOnEditorActionListener { _, _, _ -> sendQuestion(); true }
    }

    private fun sendQuestion() {
        val ui = binding ?: return
        if (sending) return
        if (!NetworkStatus.isOnline(requireContext())) {
            addMessage("You're offline. Connect to the internet, then try your question again.", false)
            return
        }
        if (FirebaseAuth.getInstance().currentUser == null) {
            addMessage("Sign in to use shopping help. Tap here to sign in.", false, signIn = true)
            return
        }
        val question = ui.assistantQuestion.text.toString().trim()
        if (question.length !in 3..500) {
            ui.assistantQuestion.error = "Enter 3 to 500 characters"
            return
        }
        addMessage(question, true)
        ui.assistantQuestion.text.clear()
        sending = true
        val startedAt = SystemClock.elapsedRealtime()
        AppAnalytics.get().track(AnalyticsEvent(AnalyticsNames.ASSISTANT_REQUEST))
        ui.assistantSend.isEnabled = false
        ui.assistantProgress.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = FirebaseFunctions.getInstance("asia-south1")
                    .getHttpsCallable("askShoppingAssistant")
                    .call(mapOf("question" to question)).await()
                val data = result.data as? Map<*, *> ?: error("Empty assistant response")
                addMessage(data["answer"] as? String ?: "No answer available.", false)
                (data["products"] as? List<*>)?.forEach { raw ->
                    (raw as? Map<*, *>)?.let(::addProduct)
                }
                AppAnalytics.get().track(AnalyticsEvent(AnalyticsNames.ASSISTANT_RESPONSE,
                    mapOf(AnalyticsParams.RESPONSE_LATENCY_MS to
                        (SystemClock.elapsedRealtime() - startedAt),
                        AnalyticsParams.RECOMMENDATION_COUNT to
                            ((data["products"] as? List<*>)?.size ?: 0))))
                (data["remainingToday"] as? Number)?.let { remaining ->
                    addMessage("${remaining.toInt()} questions left today", false)
                }
            } catch (error: Exception) {
                AppAnalytics.get().track(AnalyticsEvent(AnalyticsNames.ASSISTANT_ERROR,
                    mapOf(AnalyticsParams.ERROR_CODE to
                        ((error as? FirebaseFunctionsException)?.code?.name ?: "unknown"))))
                val message = when ((error as? FirebaseFunctionsException)?.code) {
                    FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                        "Today's shopping help limit has been reached. Please try tomorrow."
                    FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Please sign in again."
                    else -> "Shopping help is unavailable right now. Please try again later."
                }
                addMessage(message, false)
            } finally {
                sending = false
                binding?.assistantSend?.isEnabled = true
                binding?.assistantProgress?.visibility = View.GONE
            }
        }
    }

    private fun addMessage(message: String, mine: Boolean, signIn: Boolean = false) {
        val entry = AssistantEntry.Message(message, mine, signIn)
        conversation.entries.add(entry)
        renderMessage(entry)
    }

    private fun renderMessage(entry: AssistantEntry.Message) {
        val ui = binding ?: return
        val context = requireContext()
        val bubble = TextView(context).apply {
            text = entry.text
            textSize = 15f
            setTextColor(if (entry.mine) Color.WHITE else Color.rgb(33, 33, 33))
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (entry.mine) Color.rgb(41, 98, 255) else Color.WHITE)
            if (entry.signIn) {
                setOnClickListener { findNavController().navigate(R.id.loginFragment) }
                isClickable = true
            }
        }
        ui.assistantMessages.addView(bubble, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (entry.mine) Gravity.END else Gravity.START
            setMargins(0, 0, 0, dp(10))
        })
        ui.assistantScroll.post { ui.assistantScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addProduct(product: Map<*, *>) {
        val legacyId = (product["legacyId"] as? Number)?.toInt() ?: return
        if (legacyId <= 0) return
        val name = product["name"] as? String ?: return
        val priceMinor = (product["priceMinor"] as? Number)?.toLong() ?: return
        val entry = AssistantEntry.Product(legacyId, name, priceMinor,
            product["imageUrl"] as? String ?: "")
        conversation.entries.add(entry)
        renderProduct(entry)
    }

    private fun renderProduct(entry: AssistantEntry.Product) {
        val ui = binding ?: return
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(Color.WHITE)
            setOnClickListener {
                AppAnalytics.get().track(AnalyticsEvent(AnalyticsNames.ASSISTANT_SELECT_ITEM,
                    mapOf(AnalyticsParams.ITEM_ID to "legacy:${entry.legacyId}")))
                findNavController().navigate(R.id.productDetailFragment,
                    Bundle().apply { putInt("productId", entry.legacyId) })
            }
        }
        val image = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (entry.imageUrl.isNotBlank()) Glide.with(this).load(entry.imageUrl).into(this)
        }
        row.addView(image)
        row.addView(TextView(requireContext()).apply {
            text = "${entry.name}\n${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(entry.priceMinor / 100.0)}"
            textSize = 15f
            setTextColor(Color.rgb(33, 33, 33))
            setPadding(dp(12), 0, 0, 0)
        })
        ui.assistantMessages.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(10)) })
        ui.assistantScroll.post { ui.assistantScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(14).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

internal sealed interface AssistantEntry {
    data class Message(val text: String, val mine: Boolean, val signIn: Boolean) : AssistantEntry
    data class Product(val legacyId: Int, val name: String, val priceMinor: Long,
        val imageUrl: String) : AssistantEntry
}

class ShoppingAssistantViewModel : ViewModel() {
    internal val entries = mutableListOf<AssistantEntry>()
}
