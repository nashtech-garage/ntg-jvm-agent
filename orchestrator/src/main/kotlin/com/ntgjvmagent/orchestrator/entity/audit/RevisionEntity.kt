package com.ntgjvmagent.orchestrator.entity.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import java.io.Serializable
import java.util.UUID

/**
 * Custom Revision Entity for Hibernate Envers.
 * Tracks who made changes and when for audit purposes.
 */
@Entity
@Table(name = "revinfo")
@org.hibernate.envers.RevisionEntity(EnversRevisionListener::class)
class RevisionEntity : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revision_seq")
    @SequenceGenerator(name = "revision_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    var id: Int? = null

    @RevisionTimestamp
    @Column(name = "revtstmp")
    var timestamp: Long? = null

    @Column(name = "username", length = 255)
    var username: String? = null

    @Column(name = "user_id")
    var userId: UUID? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RevisionEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
