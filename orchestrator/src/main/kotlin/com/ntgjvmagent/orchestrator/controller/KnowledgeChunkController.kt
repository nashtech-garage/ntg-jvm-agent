package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkRequestDto
import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.service.KnowledgeChunkService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/knowledge/{knowledgeId}/chunks")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Knowledge Chunks", description = "Manage text chunks and embeddings for knowledge sources")
class KnowledgeChunkController(
    private val chunkService: KnowledgeChunkService,
    private val knowledgeService: KnowledgeImportService,
) {
    @GetMapping
    @Operation(summary = "List all chunks for a knowledge source")
    fun listChunks(
        @PathVariable knowledgeId: UUID,
    ): List<KnowledgeChunkResponseDto> = chunkService.getByKnowledge(knowledgeId)

    @GetMapping("/count")
    @Operation(summary = "Count all chunks for a knowledge source")
    fun countChunks(
        @PathVariable knowledgeId: UUID,
    ): Long = chunkService.countByKnowledge(knowledgeId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a single chunk manually")
    fun createChunk(
        @PathVariable knowledgeId: UUID,
        @Valid @RequestBody req: KnowledgeChunkRequestDto,
    ): KnowledgeChunkResponseDto =
        chunkService
            .addChunk(
                knowledgeId = knowledgeId,
                content = req.content,
                metadata = req.metadata,
            )

    @PutMapping("/{chunkId}")
    @Operation(summary = "Update a chunk's content and metadata")
    fun updateChunk(
        @PathVariable chunkId: UUID,
        @Valid @RequestBody req: KnowledgeChunkRequestDto,
    ): KnowledgeChunkResponseDto =
        chunkService.updateChunk(
            chunkId = chunkId,
            newContent = req.content,
            newMetadata = req.metadata,
        )

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import a document and split into chunks")
    fun importFile(
        @PathVariable knowledgeId: UUID,
        @RequestParam file: MultipartFile,
    ): KnowledgeImportingResponseVm = knowledgeService.importDocument(knowledgeId, file)

    @GetMapping("/search")
    @Operation(summary = "Search similar chunks by text query")
    fun searchSimilarChunks(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") topK: Int,
    ) = chunkService
        .searchSimilarChunks(query, topK)
}
