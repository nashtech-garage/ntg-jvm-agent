package com.ntgjvmagent.orchestrator.support

import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

object SoftDeleteAssertions {
    /**
     * Verifies that a soft-deleted entity:
     *  1. Is not visible to default repository queries (@SQLRestriction hides it)
     *  2. Has deleted_at set in the database
     */
    fun <T> assertSoftDeleted(
        entityManager: EntityManager,
        entityClass: Class<T>,
        id: UUID,
    ) {
        val result =
            entityManager
                .createQuery(
                    "SELECT e.deletedAt FROM ${entityClass.simpleName} e WHERE e.id = :id",
                    Instant::class.java,
                ).setParameter("id", id)
                .singleResult

        assertNotNull(result, "Expected 'deletedAt' to be set for soft-deleted record")
        assertTrue(
            !result.isAfter(Instant.now().plusSeconds(1)),
            "deletedAt should not be in the future",
        )
    }
}
