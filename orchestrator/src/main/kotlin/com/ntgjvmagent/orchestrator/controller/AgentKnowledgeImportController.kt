package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.request.FileKnowledgeImportConfigRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentKnowledgeImportResponseDto
import com.ntgjvmagent.orchestrator.ingestion.worker.FileImportWorker
import com.ntgjvmagent.orchestrator.model.FileKnowledgeInternalRequest
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
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
    private val fileImportWorker: FileImportWorker,
) {
    @PostMapping(
        path = ["/import"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun importFiles(
        @PathVariable agentId: UUID,
        @RequestPart("files") files: List<MultipartFile>,
        @Valid @ModelAttribute config: FileKnowledgeImportConfigRequestDto,
    ): AgentKnowledgeImportResponseDto {
        require(files.isNotEmpty()) { "At least one file must be provided." }

        val knowledge =
            agentKnowledgeService.createFileKnowledge(
                agentId,
                FileKnowledgeInternalRequest(
                    name = config.name,
                    metadata = config.metadata,
                ),
            )

        // enqueue async import task
        files.forEach { file ->
            fileImportWorker.run(agentId, knowledge.id, file)
        }

        return AgentKnowledgeImportResponseDto(
            knowledge = knowledge,
            fileNames = files.map { it.originalFilename ?: "file" },
        )
    }
}
