package handler

import service.TimeService

class TimeHandler(private val timeService: TimeService = TimeService()) {
    private var requestCount = 0

    fun handle(request: String): String {
        requestCount++
        return when (request.trim().uppercase()) {
            "GET TIME" -> {
                val currentTime = timeService.getCurrentTime()
                "TIME $currentTime UTC\n"
            }
            "GET STATS" -> "STATS Requests handled: $requestCount\n"
            else -> "ERROR Unknown command\n"
        }
    }

    // Useful for testing or monitoring
    fun getRequestCount(): Int = requestCount
}
