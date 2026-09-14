package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.request.MemoryPreferenceRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentMemoryResponseDto
import com.ntgjvmagent.orchestrator.dto.response.MemoryDeletionResponseDto
import com.ntgjvmagent.orchestrator.dto.response.MemoryPreferenceResponseDto
import com.ntgjvmagent.orchestrator.service.AgentMemoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/memories")
@Tag(
    name = "Long-term memory",
    description =
        "Opt-in assistant memory. Deleting memory removes database records " +
            "but cannot remove copies already captured in application logs.",
)
class AgentMemoryController(
    private val memoryService: AgentMemoryService,
) {
    @GetMapping
    @Operation(summary = "List all long-term memories owned by the authenticated user")
    fun list(): ResponseEntity<List<AgentMemoryResponseDto>> = ResponseEntity.ok(memoryService.listOwnMemories())

    @GetMapping("/preference")
    @Operation(summary = "Read the authenticated user's long-term memory opt-in setting")
    fun getPreference(): ResponseEntity<MemoryPreferenceResponseDto> =
        ResponseEntity.ok(MemoryPreferenceResponseDto(memoryService.isEnabled()))

    @PutMapping("/preference")
    @Operation(summary = "Enable or disable long-term memory for the authenticated user")
    fun setPreference(
        @RequestBody request: MemoryPreferenceRequestDto,
    ): ResponseEntity<MemoryPreferenceResponseDto> =
        ResponseEntity.ok(MemoryPreferenceResponseDto(memoryService.setEnabled(request.enabled)))

    @DeleteMapping("/{memoryId}")
    @Operation(
        summary = "Delete one owned memory",
        description =
            "Soft-deletes the database record. " +
                "This operation cannot remove copies already captured in application logs.",
    )
    fun deleteOne(
        @PathVariable memoryId: UUID,
    ): ResponseEntity<MemoryDeletionResponseDto> =
        ResponseEntity.ok(
            MemoryDeletionResponseDto(
                deletedCount = memoryService.deleteById(memoryId),
                logRetentionNotice = AgentMemoryService.LOG_RETENTION_NOTICE,
            ),
        )

    @DeleteMapping
    @Operation(
        summary = "Delete all memories owned by the authenticated user",
        description =
            "Soft-deletes all database records owned by this user. " +
                "This operation cannot remove copies already captured in application logs.",
    )
    fun deleteAll(): ResponseEntity<MemoryDeletionResponseDto> =
        ResponseEntity.ok(
            MemoryDeletionResponseDto(
                deletedCount = memoryService.deleteAll(),
                logRetentionNotice = AgentMemoryService.LOG_RETENTION_NOTICE,
            ),
        )
}
