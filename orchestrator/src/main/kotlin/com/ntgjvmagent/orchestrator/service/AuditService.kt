package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.entity.SystemSetting
import com.ntgjvmagent.orchestrator.entity.audit.RevisionEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Service for querying audit history using Hibernate Envers.
 */
@Service
@Transactional(readOnly = true)
class AuditService(
    @PersistenceContext private val entityManager: EntityManager,
) {

    private fun auditReader(): AuditReader = AuditReaderFactory.get(entityManager)

    fun getRevisions(entityClass: Class<*>, entityId: UUID): List<Number> =
        auditReader().getRevisions(entityClass, entityId)

    fun <T> findEntityAtRevision(entityClass: Class<T>, entityId: UUID, revision: Number): T? =
        auditReader().find(entityClass, entityId, revision)

    fun <T> getRevisionHistory(entityClass: Class<T>): List<RevisionInfo<T>> {
        val results = auditReader()
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .addOrder(AuditEntity.revisionNumber().desc())
            .resultList

        return results.map { row ->
            val arr = row as Array<*>
            @Suppress("UNCHECKED_CAST")
            RevisionInfo(
                entity = arr[0] as T,
                revisionEntity = arr[1] as RevisionEntity,
                revisionType = arr[2] as RevisionType,
            )
        }
    }

    /**
     * History of entity revisions.
     * Each element returns: entity snapshot + revision metadata + revision type.
     */
    fun <T> getRevisionHistory(entityClass: Class<T>, entityId: UUID): List<RevisionInfo<T>> {
        val results = auditReader()
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().eq(entityId))
            .addOrder(AuditEntity.revisionNumber().desc())
            .resultList

        return results.map { row ->
            val arr = row as Array<*>
            @Suppress("UNCHECKED_CAST")
            RevisionInfo(
                entity = arr[0] as T,
                revisionEntity = arr[1] as RevisionEntity,
                revisionType = arr[2] as RevisionType,
            )
        }
    }

    fun <T> getRevisionHistoryBetween(
        entityClass: Class<T>,
        entityId: UUID,
        startTime: Instant,
        endTime: Instant,
    ): List<RevisionInfo<T>> {
        val results = auditReader()
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().eq(entityId))
            // Note: the RevisionEntity timestamp is mapped to the "revtstmp" column.
            .add(AuditEntity.revisionProperty("revtstmp").ge(startTime.toEpochMilli()))
            .add(AuditEntity.revisionProperty("revtstmp").le(endTime.toEpochMilli()))
            .addOrder(AuditEntity.revisionNumber().desc())
            .resultList

        return results.map { row ->
            val arr = row as Array<*>
            @Suppress("UNCHECKED_CAST")
            RevisionInfo(
                entity = arr[0] as T,
                revisionEntity = arr[1] as RevisionEntity,
                revisionType = arr[2] as RevisionType,
            )
        }
    }

    fun getRevisionsWhenPropertyChanged(
        entityClass: Class<*>,
        entityId: UUID,
        propertyName: String,
    ): List<Number> {
        val results = auditReader()
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().eq(entityId))
            .add(AuditEntity.property(propertyName).hasChanged())
            .addOrder(AuditEntity.revisionNumber().asc())
            .resultList

        return results.map { row ->
            val arr = row as Array<*>
            arr[1] as Number // revision number
        }
    }

    fun getLatestRevision(entityClass: Class<*>, entityId: UUID): Int? {
        val revisions = getRevisions(entityClass, entityId)
        return revisions.map { it.toInt() }.maxOrNull()
    }

    fun wasModifiedByUser(
        entityClass: Class<*>,
        entityId: UUID,
        username: String,
    ): Boolean {
        val results = auditReader()
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().eq(entityId))
            .add(AuditEntity.revisionProperty("username").eq(username))
            .setMaxResults(1)
            .resultList

        return results.isNotEmpty()
    }

    fun getEntitiesModifiedAtRevision(revision: Number): Map<String, List<Any>> = emptyMap()

    fun getAllAuditLogs(agentId: UUID? = null): List<AuditLogVm> {
        val collected = mutableListOf<AuditLogVm>()

        fun <T> collect(entityClass: Class<T>, entityType: String) {
            val items = getRevisionHistory(entityClass)
            items.forEach { info ->
                val entityId = extractId(info.entity)
                if (agentId == null || (entityType == "Agent" && entityId == agentId)) {
                    collected += AuditLogVm(
                        entityType = entityType,
                        entityId = entityId,
                        revision = info.revisionEntity.id ?: 0,
                        revisionType = info.revisionType.name,
                        username = info.revisionEntity.username,
                        timestamp = info.revisionEntity.timestamp?.let { Instant.ofEpochMilli(it) },
                        payload = toSafePayload(info.entity),
                    )
                }
            }
        }

        collect(com.ntgjvmagent.orchestrator.entity.agent.Agent::class.java, "Agent")
        collect(com.ntgjvmagent.orchestrator.entity.Tool::class.java, "Tool")
        collect(Conversation::class.java, "Conversation")
        collect(SystemSetting::class.java, "SystemSetting")

        return collected.sortedByDescending { it.revision }
    }

    private fun extractId(entity: Any?): UUID? =
        when (entity) {
            null -> null
            is com.ntgjvmagent.orchestrator.entity.agent.Agent -> entity.id
            is com.ntgjvmagent.orchestrator.entity.Tool -> entity.id
            is Conversation -> entity.id
            is SystemSetting -> entity.id
            else -> null
        }

    private fun toSafePayload(entity: Any?): Any? =
        when (entity) {
            null -> null
            is com.ntgjvmagent.orchestrator.entity.agent.Agent -> mapOf(
                "id" to entity.id,
                "name" to entity.name,
                "description" to entity.description,
                "provider" to entity.provider,
                "baseUrl" to entity.baseUrl,
                "chatCompletionsPath" to entity.chatCompletionsPath,
                "model" to entity.model,
                "temperature" to entity.temperature,
                "maxTokens" to entity.maxTokens,
                "topP" to entity.topP,
                "frequencyPenalty" to entity.frequencyPenalty,
                "presencePenalty" to entity.presencePenalty,
                "settings" to entity.settings,
                "version" to entity.version,
            )
            is com.ntgjvmagent.orchestrator.entity.Tool -> mapOf(
                "id" to entity.id,
                "name" to entity.name,
                "type" to entity.type,
                "description" to entity.description,
                "definition" to entity.definition,
                "connectionConfig" to entity.connectionConfig,
                "baseUrl" to entity.baseUrl,
            )
            is Conversation -> mapOf(
                "id" to entity.id,
                "title" to entity.title,
                "isActive" to entity.isActive,
            )
            is SystemSetting -> mapOf(
                "id" to entity.id,
                "siteName" to entity.siteName,
                "maintenanceMode" to entity.maintenanceMode,
                "maximumUser" to entity.maximumUser,
                "sessionTimeout" to entity.sessionTimeout,
                "maximumSizeFileUpload" to entity.maximumSizeFileUpload,
                "allowedFileTypes" to entity.allowedFileTypes,
                "userRegistration" to entity.userRegistration,
                "emailVerification" to entity.emailVerification,
            )
            else -> null
        }
}

data class RevisionInfo<T>(
    val entity: T,
    val revisionEntity: RevisionEntity,
    val revisionType: RevisionType,
)

data class AuditLogVm(
    val entityType: String,
    val entityId: UUID?,
    val revision: Int,
    val revisionType: String,
    val username: String?,
    val timestamp: Instant?,
    val payload: Any?,
)
