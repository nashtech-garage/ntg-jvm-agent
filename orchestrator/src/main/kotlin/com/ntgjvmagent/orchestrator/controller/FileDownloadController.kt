package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.KnowledgeFileStorageService
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path

@RestController
@RequestMapping("/api/files")
class FileDownloadController(
    private val fileStorageService: KnowledgeFileStorageService,
) {
    @GetMapping("/download")
    fun download(
        @RequestParam("path") path: String,
    ): ResponseEntity<UrlResource> {
        val p: Path = fileStorageService.loadFile(path)
        if (!Files.exists(p)) {
            return ResponseEntity.notFound().build()
        }

        val resource = UrlResource(p.toUri())
        val contentType = Files.probeContentType(p) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${p.fileName}\"")
            .body(resource)
    }
}
