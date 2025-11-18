package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeImportResponseDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
import com.ntgjvmagent.orchestrator.service.KnowledgeImportService
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}/knowledge/import")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AgentKnowledgeImportController(
    private val agentKnowledgeService: AgentKnowledgeService,
    private val knowledgeImportService: KnowledgeImportService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDocument(
        @PathVariable agentId: UUID,
        @RequestPart("file") file: MultipartFile,
        @RequestPart("json") request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeImportResponseDto {
        val knowledge = agentKnowledgeService.create(agentId, request)

        val importResult =
            knowledgeImportService.importDocument(
                agentId = agentId,
                knowledgeId = knowledge.id,
                file = file,
            )

        return AgentKnowledgeImportResponseDto(
            knowledge = knowledge,
            numberOfChunks = importResult.numberOfSegment,
            fileName = importResult.originalFilename,
        )
    }
}
