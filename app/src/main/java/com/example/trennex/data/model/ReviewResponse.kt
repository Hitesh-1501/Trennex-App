package com.example.trennex.data.model

data class ReviewResponse(
    val ratings : Int,
    val comment: String,
    val date: String,
    val reviewerName: String
)