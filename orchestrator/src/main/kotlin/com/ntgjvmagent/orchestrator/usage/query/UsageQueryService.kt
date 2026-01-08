package com.ntgjvmagent.orchestrator.usage.query

import com.ntgjvmagent.orchestrator.model.UsageGroupBy
import com.ntgjvmagent.orchestrator.repository.UsageAggregationRepository
import com.ntgjvmagent.orchestrator.usage.query.dto.response.agent.UsageByAgentResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.agent.UsageByAgentRowDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.freshness.UsageFreshnessDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.summary.UsageSummaryResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.timeseries.UsageTimeSeriesPointDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.timeseries.UsageTimeSeriesResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.user.UsageByUserResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.user.UsageByUserRowDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsageQueryService(
    private val usageRepository: UsageAggregationRepository,
) {
    fun getSummary(
        from: LocalDate,
        to: LocalDate,
    ): UsageSummaryResponseDto {
        val usage = usageRepository.sumUsage(from, to)

        return UsageSummaryResponseDto(
            totalTokens = usage.totalTokens,
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
        )
    }

    fun getDailyTimeSeries(
        from: LocalDate,
        to: LocalDate,
    ): UsageTimeSeriesResponseDto {
        require(!from.isAfter(to)) {
            INVALID_DATE_RANGE_MESSAGE
        }

        val points =
            usageRepository
                .aggregateDaily(from, to)
                .map {
                    UsageTimeSeriesPointDto(
                        date = it.usageDate,
                        totalTokens = it.totalTokens,
                        promptTokens = it.promptTokens,
                        completionTokens = it.completionTokens,
                    )
                }

        return UsageTimeSeriesResponseDto(
            groupBy = UsageGroupBy.DAY,
            points = points,
        )
    }

    fun getUsageByAgent(
        from: LocalDate,
        to: LocalDate,
    ): UsageByAgentResponseDto {
        require(!from.isAfter(to)) {
            INVALID_DATE_RANGE_MESSAGE
        }

        val rows =
            usageRepository
                .aggregateByAgent(from, to)
                .map {
                    UsageByAgentRowDto(
                        agentId = it.agentId,
                        agentName = it.agentName,
                        promptTokens = it.promptTokens,
                        completionTokens = it.completionTokens,
                        totalTokens = it.totalTokens,
                    )
                }

        return UsageByAgentResponseDto(
            from = from,
            to = to,
            rows = rows,
        )
    }

    fun getUsageByUser(
        from: LocalDate,
        to: LocalDate,
    ): UsageByUserResponseDto {
        require(!from.isAfter(to)) {
            INVALID_DATE_RANGE_MESSAGE
        }

        val rows =
            usageRepository
                .aggregateByUser(from, to)
                .map {
                    UsageByUserRowDto(
                        userId = it.userId,
                        userName = it.userName,
                        promptTokens = it.promptTokens,
                        completionTokens = it.completionTokens,
                        totalTokens = it.totalTokens,
                    )
                }

        return UsageByUserResponseDto(
            from = from,
            to = to,
            rows = rows,
        )
    }

    fun getFreshness(): UsageFreshnessDto {
        val latest = usageRepository.findLatestAggregatedDate()

        return UsageFreshnessDto(
            latestAggregatedDate = latest,
        )
    }

    companion object {
        const val INVALID_DATE_RANGE_MESSAGE =
            "`from` must be before or equal to `to`"
    }
}
