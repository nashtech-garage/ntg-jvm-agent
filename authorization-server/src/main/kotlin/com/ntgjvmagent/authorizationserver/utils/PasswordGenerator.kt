package com.ntgjvmagent.authorizationserver.utils

import java.util.*

object PasswordGenerator {
    fun generateTempPassword(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = Random()
        return (0 until length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}

