package com.ntgjvmagent.orchestrator.usage.aggregate.service

import com.ntgjvmagent.orchestrator.repository.UsageAggregationRepository
import com.ntgjvmagent.orchestrator.repository.UsageAggregationStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UsageAggregationService(
    private val stateRepo: UsageAggregationStateRepository,
    private val aggRepo: UsageAggregationRepository,
) {
    /**
     * Runs daily usage aggregation.
     *
     * Invariant:
     * - token_usage_log.created_at always reflects insert time (now)
     * - usage for a day never changes after the day ends
     *
     * Therefore:
     * - today is never aggregated
     * - past days are immutable once aggregated
     */
    @Transactional
    fun runAggregation(todayUtc: LocalDate) {
        val targetDay = todayUtc.minusDays(1)

        recoverMissingDaysInternal(targetDay)
    }

    /**
     * Aggregates all missing finalized days up to the given target day.
     *
     * This method is idempotent and safe to re-run:
     * - days are aggregated exactly once
     * - gaps caused by downtime are automatically recovered
     */
    private fun recoverMissingDaysInternal(targetDay: LocalDate) {
        val lastProcessed = stateRepo.getLastProcessedDate()

        if (lastProcessed.isBefore(targetDay)) {
            lastProcessed
                .plusDays(1)
                .datesUntil(targetDay.plusDays(1))
                .forEach { day ->
                    aggRepo.recomputeDay(day)
                }

            stateRepo.updateLastProcessedDate(targetDay)
        }
    }
}
