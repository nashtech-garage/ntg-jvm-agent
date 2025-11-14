package com.ntgjvmagent.orchestrator.viewmodel

import com.ntgjvmagent.orchestrator.validation.ValidChatFile
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@ValidChatFile
data class ChatRequestVm(
    @field:NotBlank(message = "Question must not be blank")
    val question: String,
    val conversationId: UUID?,
    @field:Size(max = 3, message = "Maximum 3 files allowed")
    val files: List<MultipartFile>?,
)
