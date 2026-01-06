package com.ntgjvmagent.orchestrator.support

import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

object SoftDeleteAssertions {
    /**
     * Verifies that a soft-deleted entity:
     *  1. Still exists in DB
     *  2. Has deleted_at set
     */
    fun assertSoftDeleted(
        entityManager: EntityManager,
        tableName: String,
        id: UUID,
    ) {
        val deletedAt =
            entityManager
                .createNativeQuery(
                    "SELECT deleted_at FROM $tableName WHERE id = :id",
                ).setParameter("id", id)
                .singleResult as Instant?

        assertNotNull(deletedAt, "Expected deleted_at to be set for soft-deleted record")
        assertTrue(
            !deletedAt.isAfter(Instant.now().plusSeconds(1)),
            "deleted_at should not be in the future",
        )
    }
}
