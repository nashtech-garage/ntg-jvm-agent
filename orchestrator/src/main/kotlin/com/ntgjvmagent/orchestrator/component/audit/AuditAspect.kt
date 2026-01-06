package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.utils.SnapshotUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Aspect
@Component
class AuditAspect(
    private val publisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(audit)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        audit: Audit,
    ): Any {
        val actor =
            try {
                AuditContext.get()
            } catch (ex: Exception) {
                logger.warn("AuditContext missing; proceeding without audit - method={}", joinPoint.signature)
                null
            }

        val result =
            try {
                joinPoint.proceed()
            } catch (ex: Throwable) {
                // let original exception bubble up, but log for visibility
                logger.error("Exception during audited method proceed: {}", joinPoint.signature, ex)
                throw ex
            }

        val resourceId = SnapshotUtils.extractId(joinPoint, result)

        logger.info(
            "Audit - Actor: {}, Action: {}, Resource: {}, ResourceId: {}, NewValue: {}",
            actor?.id,
            audit.action,
            audit.resource,
            resourceId,
            result,
        )

        if (actor != null) {
            try {
                publisher.publishEvent(
                    AuditEvent(
                        actor = actor,
                        action = audit.action,
                        resourceType = audit.resource,
                        resourceId = resourceId,
                    ),
                )
            } catch (ex: Exception) {
                // do not impede business flow; log error for debugging
                logger.error("Failed to publish AuditEvent for method={}", joinPoint.signature, ex)
            }
        }
        return result
    }
}
