package com.ntgjvmagent.orchestrator.dto

/**
 * DTO representing a single audit history entry returned by the audit endpoints.
 *
 * revision: the integer revision id from Envers (or 0 if unavailable)
 * revisionType: the Envers revision type (ADD, MOD, DEL, etc.)
 * entity: the entity snapshot at that revision (may be null)
 */
data class AuditHistoryResponse(
    val revision: Int,
    val revisionType: String,
    val entity: Any?,
)

