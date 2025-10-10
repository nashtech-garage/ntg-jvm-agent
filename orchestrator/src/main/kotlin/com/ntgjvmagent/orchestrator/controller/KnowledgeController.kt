package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.KnowledgeService
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/knowledge")
class KnowledgeController(
    private val knowledgeService: KnowledgeService,
) {
    @PostMapping()
    fun import(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<KnowledgeImportingResponseVm> = ResponseEntity.ok(knowledgeService.importDocument(file))
}
