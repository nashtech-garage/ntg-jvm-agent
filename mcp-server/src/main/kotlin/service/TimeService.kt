package service

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimeService {
    fun getCurrentTime(): String {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return now.format(formatter)
    }
}
