package com.ntgjvmagent.orchestrator.usage.aggregate.job

import com.ntgjvmagent.orchestrator.usage.aggregate.service.UsageAggregationService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class DailyUsageAggregationJob(
    private val aggregationService: UsageAggregationService,
) {
    /**
     * Primary trigger
     * Runs every day at 00:05 UTC
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    fun scheduledRun() {
        run()
    }

    /**
     * Safety trigger
     * Handles downtime, redeploys, missed schedules
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        run()
    }

    private fun run() {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        aggregationService.runAggregation(todayUtc)
    }
}
