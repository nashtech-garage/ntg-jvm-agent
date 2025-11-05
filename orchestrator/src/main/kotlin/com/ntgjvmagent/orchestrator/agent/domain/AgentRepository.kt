package com.ntgjvmagent.orchestrator.agent.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AgentRepository : JpaRepository<Agent, UUID> {
    fun findByNameIgnoreCase(name: String): Optional<Agent>

    @Query("SELECT a FROM Agent a WHERE a.deletedAt IS NULL AND a.active = true")
    fun findAllActive(): List<Agent>

    @Query("SELECT a FROM Agent a WHERE a.deletedAt IS NULL")
    fun findAllNotDeleted(): List<Agent>

    @Query("SELECT a FROM Agent a WHERE a.id = :id AND a.deletedAt IS NULL")
    fun findByIdNotDeleted(id: UUID): Optional<Agent>
}
