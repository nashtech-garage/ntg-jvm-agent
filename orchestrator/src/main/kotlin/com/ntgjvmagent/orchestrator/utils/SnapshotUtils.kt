package com.ntgjvmagent.orchestrator.utils

import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import org.aspectj.lang.ProceedingJoinPoint
import java.util.UUID

object SnapshotUtils {
    fun extractId(
        joinPoint: ProceedingJoinPoint,
        result: Any?,
    ): UUID? {
        joinPoint.args.forEach {
            when (it) {
                is AgentResponseDto -> return it.id
            }
        }

        return when (result) {
            is AgentResponseDto -> result.id
            else -> null
        }
    }
}
