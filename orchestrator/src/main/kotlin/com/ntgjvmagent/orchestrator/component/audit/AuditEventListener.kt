package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.entity.AuditLog
import com.ntgjvmagent.orchestrator.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuditEventListener(
    private val auditLogRepository: AuditLogRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: AuditEvent) {
        try {
            val auditLog =
                AuditLog(
                    actorId = event.actor.id,
                    actorName = event.actor.name,
                    action = event.action,
                    resourceType = event.resourceType.name,
                    resourceId = event.resourceId,
                )

            auditLogRepository.save(auditLog)
            logger.info(
                "Saved AuditLog for actor={} action={} resourceId={}",
                event.actor.id,
                event.action,
                event.resourceId,
            )
        } catch (ex: Exception) {
            logger.error("Failed to persist AuditEvent for actor=${event.actor.id} action=${event.action}", ex)
        }
    }
}
