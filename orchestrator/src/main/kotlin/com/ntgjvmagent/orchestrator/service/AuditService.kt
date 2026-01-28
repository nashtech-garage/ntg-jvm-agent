package com.ntgjvmagent.orchestrator.service

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
            // Lưu ý: field timestamp trong RevisionEntity được map vào column revtstmp
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
}

data class RevisionInfo<T>(
    val entity: T,
    val revisionEntity: RevisionEntity,
    val revisionType: RevisionType,
)
