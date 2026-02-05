package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.entity.SystemSetting
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.service.AuditService
import com.ntgjvmagent.orchestrator.service.AuditLogVm
import com.ntgjvmagent.orchestrator.service.RevisionInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Entity audit history and revision tracking")
class AuditController(
    private val auditService: AuditService,
) {

    @GetMapping("/agent/{id}/revisions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all revisions for an agent")
    fun getAgentRevisions(@PathVariable id: UUID): ResponseEntity<List<Number>> {
        val revisions = auditService.getRevisions(Agent::class.java, id)
        return ResponseEntity.ok(revisions)
    }

    @GetMapping("/agent/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for an agent")
    fun getAgentHistory(@PathVariable id: UUID): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Agent::class.java, id)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/agent/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for all agents")
    fun getAllAgentHistory(): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Agent::class.java)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/agent/{id}/revision/{revision}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get agent state at specific revision")
    fun getAgentAtRevision(
        @PathVariable id: UUID,
        @PathVariable revision: Int,
    ): ResponseEntity<Agent> {
        val entity = auditService.findEntityAtRevision(Agent::class.java, id, revision)
        return entity?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/agent/{id}/history/between")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get agent audit history within time range")
    fun getAgentHistoryBetween(
        @PathVariable id: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant,
    ): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistoryBetween(Agent::class.java, id, startTime, endTime)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/tool/{id}/revisions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all revisions for a tool")
    fun getToolRevisions(@PathVariable id: UUID): ResponseEntity<List<Number>> {
        val revisions = auditService.getRevisions(Tool::class.java, id)
        return ResponseEntity.ok(revisions)
    }

    @GetMapping("/tool/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for a tool")
    fun getToolHistory(@PathVariable id: UUID): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Tool::class.java, id)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/tool/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for all tools")
    fun getAllToolHistory(): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Tool::class.java)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/conversation/{id}/revisions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all revisions for a conversation")
    fun getConversationRevisions(@PathVariable id: UUID): ResponseEntity<List<Number>> {
        val revisions = auditService.getRevisions(Conversation::class.java, id)
        return ResponseEntity.ok(revisions)
    }

    @GetMapping("/conversation/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for a conversation")
    fun getConversationHistory(@PathVariable id: UUID): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Conversation::class.java, id)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/conversation/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for all conversations")
    fun getAllConversationHistory(): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(Conversation::class.java)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/system-setting/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for system settings")
    fun getSystemSettingHistory(@PathVariable id: UUID): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(SystemSetting::class.java, id)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "System-wide audit logs (optional filter by agent)")
    fun getSystemLogs(
        @RequestParam(required = false) agentId: UUID?,
    ): ResponseEntity<List<AuditLogVm>> {
        val logs = auditService.getAllAuditLogs(agentId)
        return ResponseEntity.ok(logs)
    }

    @GetMapping("/system-setting/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get complete audit history for all system settings")
    fun getAllSystemSettingHistory(): ResponseEntity<List<Map<String, Any?>>> {
        val history = auditService.getRevisionHistory(SystemSetting::class.java)
        return ResponseEntity.ok(history.map { it.toMap() })
    }

    @GetMapping("/revision/{revision}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all entities modified in a specific revision")
    fun getEntitiesAtRevision(@PathVariable revision: Int): ResponseEntity<Map<String, List<Any>>> {
        val entities = auditService.getEntitiesModifiedAtRevision(revision)
        return ResponseEntity.ok(entities)
    }

    private fun <T> RevisionInfo<T>.toMap(): Map<String, Any?> {
        return mapOf(
            "revision" to (this.revisionEntity.id ?: 0),
            "revisionType" to (this.revisionType.name),
            "entity" to (this.entity),
        )
    }
}
