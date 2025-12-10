package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.request.FileKnowledgeImportConfigRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeImportResponseDto
import com.ntgjvmagent.orchestrator.model.FileKnowledgeInternalRequest
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}/knowledge")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AgentKnowledgeImportController(
    private val agentKnowledgeService: AgentKnowledgeService,
    private val knowledgeImportService: KnowledgeImportService,
) {
    @PostMapping(
        path = ["/import"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun importFiles(
        @PathVariable agentId: UUID,
        @RequestPart("files") files: List<MultipartFile>,
        @Valid @ModelAttribute config: FileKnowledgeImportConfigRequestDto,
    ): AgentKnowledgeImportResponseDto {
        require(files.isNotEmpty()) { "At least one file must be provided." }

        // -----------------------------
        // Create knowledge row
        // -----------------------------
        val knowledge =
            agentKnowledgeService.create(
                agentId,
                FileKnowledgeInternalRequest(
                    name = config.name,
                    metadata = config.metadata,
                ),
            )

        // -----------------------------
        // Import each file (multi-file)
        // -----------------------------
        val results =
            files.map { file ->
                knowledgeImportService.importDocument(
                    agentId = agentId,
                    knowledgeId = knowledge.id,
                    file = file,
                )
            }

        // -----------------------------
        // Build response (multi-file)
        // -----------------------------
        return AgentKnowledgeImportResponseDto(
            knowledge = knowledge,
            fileNames = results.map { it.originalFilename },
            totalChunks = results.sumOf { it.numberOfSegment },
        )
    }
}
