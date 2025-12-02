package com.ntgjvmagent.orchestrator.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class KnowledgeFileStorageService {
    private val rootPath: Path = Paths.get(System.getProperty("user.dir"), "storage")

    fun saveFile(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ): Path {
        val folder = rootPath.resolve(agentId.toString()).resolve(knowledgeId.toString())
        Files.createDirectories(folder)

        val original = file.originalFilename ?: "file"
        val safeFileName = sanitizeFileName(original)
        val savePath = folder.resolve(safeFileName)

        file.inputStream.use { input ->
            Files.copy(input, savePath, StandardCopyOption.REPLACE_EXISTING)
        }

        return savePath
    }

    fun loadFile(path: String): Path = Paths.get(path)

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
