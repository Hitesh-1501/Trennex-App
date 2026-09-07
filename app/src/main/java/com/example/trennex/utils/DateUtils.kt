package com.example.trennex.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val deliveryDateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    fun calculateExpectedDeliveryDate(shippingInfo: String): Timestamp {
        val daysToAdd = when {
            shippingInfo.contains("1-2 business days", ignoreCase = true) -> 4
            shippingInfo.contains("3-5 business days", ignoreCase = true) -> 7
            shippingInfo.contains("1 week", ignoreCase = true) -> 8
            shippingInfo.contains("2 weeks", ignoreCase = true) -> 15
            shippingInfo.contains("1 month", ignoreCase = true) -> 30
            else -> 5 // Default fallback
        }
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return Timestamp(calendar.time)
    }

    fun formatDeliveryDate(timestamp: Timestamp): String {
        return deliveryDateFormat.format(timestamp.toDate())
    }
    
    fun getDeliveryDateString(shippingInfo: String): String {
        val timestamp = calculateExpectedDeliveryDate(shippingInfo)
        return formatDeliveryDate(timestamp)
    }
}
