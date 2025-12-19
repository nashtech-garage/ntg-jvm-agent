package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.component.CurrentUserProvider
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.service.ConversationCommandService
import com.ntgjvmagent.orchestrator.service.ConversationQueryService
import com.ntgjvmagent.orchestrator.service.ConversationStreamingService
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationUpdateRequestVm
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
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
    private val conversationCommandService: ConversationCommandService,
    private val conversationQueryService: ConversationQueryService,
    private val conversationStreamingService: ConversationStreamingService,
    private val currentUserProvider: CurrentUserProvider,
) {
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    fun createConversation(
        @Valid @ModelAttribute req: ChatRequestDto,
    ): Flux<ServerSentEvent<Any>> {
        val userId = currentUserProvider.getUserId()
        return conversationStreamingService.streamConversation(req, userId)
    }

    @GetMapping()
    fun getConversations(): ResponseEntity<List<ConversationResponseVm>> {
        val userId = currentUserProvider.getUserId()
        return ResponseEntity.ok(conversationQueryService.listConversationByUser(userId))
    }

    @GetMapping("/{conversationId}/messages")
    fun getMessages(
        @PathVariable conversationId: UUID,
    ): ResponseEntity<List<ChatMessageResponseVm>> =
        ResponseEntity.ok(conversationQueryService.listMessageByConversation(conversationId))

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(
        @PathVariable conversationId: UUID,
    ): ResponseEntity<Unit> {
        conversationCommandService.deleteConversation(conversationId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{conversationId}")
    fun updateConversation(
        @PathVariable conversationId: UUID,
        @Valid @RequestBody request: ConversationUpdateRequestVm,
    ): ResponseEntity<ConversationResponseVm> {
        val userId = currentUserProvider.getUserId()
        val updatedConversation =
            conversationCommandService.updateConversationTitle(
                conversationId,
                request.title,
                userId,
            )
        return ResponseEntity.ok(updatedConversation)
    }
}
