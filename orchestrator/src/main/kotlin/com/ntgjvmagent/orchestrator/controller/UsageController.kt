package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.model.UsageGroupBy
import com.ntgjvmagent.orchestrator.usage.query.UsageQueryService
import com.ntgjvmagent.orchestrator.usage.query.dto.response.agent.UsageByAgentResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.freshness.UsageFreshnessDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.summary.UsageSummaryResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.timeseries.UsageTimeSeriesResponseDto
import com.ntgjvmagent.orchestrator.usage.query.dto.response.user.UsageByUserResponseDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/usage")
@PreAuthorize("hasRole('ADMIN')")
class UsageController(
    private val usageQueryService: UsageQueryService,
) {
    @GetMapping("/summary")
    fun getSummary(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): UsageSummaryResponseDto = usageQueryService.getSummary(from, to)

    @GetMapping("/timeseries")
    fun getUsageTimeSeries(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,
        @RequestParam
        groupBy: UsageGroupBy,
    ): UsageTimeSeriesResponseDto {
        require(groupBy == UsageGroupBy.DAY) {
            "Only DAY grouping is supported currently"
        }

        require(!from.isAfter(to)) {
            "`from` must be before or equal to `to`"
        }

        return usageQueryService.getDailyTimeSeries(from, to)
    }

    @GetMapping("/by-agent")
    fun getUsageByAgent(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,
    ): UsageByAgentResponseDto = usageQueryService.getUsageByAgent(from, to)

    @GetMapping("/by-user")
    fun getUsageByUser(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,
    ): UsageByUserResponseDto = usageQueryService.getUsageByUser(from, to)

    @GetMapping("/freshness")
    fun getUsageFreshness(): UsageFreshnessDto = usageQueryService.getFreshness()
}
