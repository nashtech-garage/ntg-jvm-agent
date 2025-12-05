package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.ConversationService
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationUpdateRequestVm
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.UUID

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService,
) {
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    fun createConversation(
        @Valid @ModelAttribute req: ChatRequestVm,
        authentication: Authentication,
    ): Flux<ServerSentEvent<Any>> {
        val username = (authentication.principal as Jwt).subject
        return conversationService.streamConversation(req, username)
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
    ): ResponseEntity<Unit> {
        conversationService.deleteConversation(conversationId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{conversationId}")
    fun updateConversation(
        @PathVariable conversationId: UUID,
        @Valid @RequestBody request: ConversationUpdateRequestVm,
        authentication: Authentication,
    ): ResponseEntity<ConversationResponseVm> {
        val username = (authentication.principal as Jwt).subject
        val updatedConversation = conversationService.updateConversationTitle(conversationId, request.title, username)
        return ResponseEntity.ok(updatedConversation)
    }
}
