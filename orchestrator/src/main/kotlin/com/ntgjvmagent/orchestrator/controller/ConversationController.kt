package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.ConversationService
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createConversation(
        @Valid @ModelAttribute req: ChatRequestVm,
        authentication: Authentication,
    ): ResponseEntity<ChatResponseVm> {
        val username = (authentication.principal as Jwt).subject
        return ResponseEntity.ok(conversationService.createConversation(req, username))
    }

    @GetMapping("/user")
    fun getConversations(authentication: Authentication): ResponseEntity<List<ConversationResponseVm>> {
        val username = (authentication.principal as Jwt).subject
        return ResponseEntity.ok(conversationService.listConversationByUser(username))
    }

    @GetMapping("/{conversationId}/messages")
    fun getMessages(
        @PathVariable conversationId: UUID,
    ): ResponseEntity<List<ChatMessageResponseVm>> =
        ResponseEntity.ok(conversationService.listMessageByConversation(conversationId))

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(
        @PathVariable conversationId: UUID,
    ): ResponseEntity<Void> {
        conversationService.deleteConversation(conversationId)
        return ResponseEntity.noContent().build()
    }
}
