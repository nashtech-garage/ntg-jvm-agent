package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.ConversationService
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.ChatResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/conversations")
class ConversationController(
    private val conversationService: ConversationService,
) {
    @PostMapping
    fun createConversation(
        @Valid @RequestBody req: ChatRequestVm,
    ): ResponseEntity<ChatResponseVm> = ResponseEntity.ok(conversationService.createConversation(req))

    @GetMapping("/user/{username}")
    fun getConversations(
        @PathVariable username: String,
    ): ResponseEntity<List<ConversationResponseVm>> =
        ResponseEntity.ok(conversationService.listConversationByUser(username))

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
