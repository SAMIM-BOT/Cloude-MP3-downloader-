package com.example.data.model

data class UserProfile(
    val uid: String = "guest_user_123",
    val username: String = "Music Lover",
    val email: String = "ytbynebula@gmail.com", // defaulted to original user email
    val joinedTimestamp: Long = System.currentTimeMillis(),
    val totalDownloadsCount: Int = 0,
    val deviceModel: String = android.os.Build.MODEL,
    val deviceBrand: String = android.os.Build.BRAND
)
