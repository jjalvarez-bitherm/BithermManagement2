package com.bithermmanagement.core.data

data class LoginConfig(
    val splashDuration: Int,
    val loginTitle: String,
    val usernameHint: String,
    val passwordHint: String,
    val loginButtonText: String,
    val forgotPasswordText: String,
    val biometricEnabled: Boolean
) 