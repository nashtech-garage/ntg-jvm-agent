package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.request.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.dto.response.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.ingestion.FileImportWorker
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}/knowledge/{knowledgeId}/chunks")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(
    name = "Knowledge Chunks",
    description = "Manage text chunks and embeddings for knowledge sources of an agent",
)
class KnowledgeChunkController(
    private val chunkService: KnowledgeChunkService,
    private val fileImportWorker: FileImportWorker,
) {
    @GetMapping
    @Operation(summary = "List all chunks for a knowledge source of an agent")
    fun listChunks(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ): List<KnowledgeChunkResponseDto> = chunkService.getByKnowledge(agentId, knowledgeId)

    @GetMapping("/count")
    @Operation(summary = "Count all chunks for a knowledge source of an agent")
    fun countChunks(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ): Long = chunkService.countByKnowledge(agentId, knowledgeId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a single chunk manually")
    fun createChunk(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
        @Valid @RequestBody req: KnowledgeChunkRequestDto,
    ): KnowledgeChunkResponseDto =
        chunkService.addChunk(
            agentId = agentId,
            knowledgeId = knowledgeId,
            content = req.content,
            metadata = req.metadata,
        )

    @PutMapping("/{chunkId}")
    @Operation(summary = "Update a chunk's content and metadata")
    fun updateChunk(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
        @PathVariable chunkId: UUID,
        @Valid @RequestBody req: KnowledgeChunkRequestDto,
    ): KnowledgeChunkResponseDto =
        chunkService.updateChunk(
            agentId = agentId,
            knowledgeId = knowledgeId,
            chunkId = chunkId,
            newContent = req.content,
            newMetadata = req.metadata,
        )

    // ------------------------------------------------------------------
    // ASYNC FILE IMPORT
    // ------------------------------------------------------------------
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Import a document file and enqueue async chunk processing")
    fun importFile(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
        @RequestPart("file") @NotNull file: MultipartFile,
    ): Map<String, String> {
        // ---- Validate input ----
        if (file.isEmpty) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Uploaded file is empty or missing",
            )
        }

        // ---- Queue async import ----
        fileImportWorker.run(agentId, knowledgeId, file)

        return mapOf(
            "originalFilename" to (file.originalFilename ?: "uploaded-file"),
        )
    }

    @GetMapping("/search")
    @Operation(summary = "Search similar chunks by text query")
    fun searchSimilarChunks(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") topK: Int,
    ): List<KnowledgeChunkResponseDto> = chunkService.searchSimilarChunks(agentId, knowledgeId, query, topK)
}
