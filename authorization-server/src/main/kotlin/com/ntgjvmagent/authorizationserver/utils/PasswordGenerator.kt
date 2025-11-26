package com.ntgjvmagent.authorizationserver.utils

import java.security.SecureRandom

/**
 * Generates a temporary password for new user accounts.
 *
 * @param length The length of the password to generate (default: 6)
 *
 */
object PasswordGenerator {
    fun generateTempPassword(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        // Use SecureRandom for cryptographically secure password generation (OWASP recommendation)
        val random = SecureRandom()
        return (0 until length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
