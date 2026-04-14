package com.example.trennex.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormator {
    private val indianFormatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    fun formatInr(amount: Double): String{
        return "₹${indianFormatter.format(amount)}"
    }
}