package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.ConversationShareService
import com.ntgjvmagent.orchestrator.viewmodel.ConversationShareResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ShareConversationRequest
import com.ntgjvmagent.orchestrator.viewmodel.SharedConversationViewVm
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/share/shared-conversations")
class SharedConversationController(
    private val conversationShareService: ConversationShareService,
) {
    /**
     * Get a shared conversation by share token (public endpoint - no authentication required)
     * @param shareToken Unique token for accessing the shared conversation
     * @return Shared conversation with messages
     */
    @GetMapping("/{shareToken}")
    fun getSharedConversation(
        @PathVariable shareToken: String,
    ): ResponseEntity<SharedConversationViewVm> {
        val sharedConversation = conversationShareService.getSharedConversation(shareToken)
        return ResponseEntity.ok(sharedConversation)
    }

    /**
     * Share a conversation with others (requires authentication)
     * @param conversationId ID of the conversation to share
     * @param request Share request containing expiry days
     * @param authentication Current user authentication
     * @return Share response with share token and URL
     */
    @PostMapping("/{conversationId}/share")
    fun shareConversation(
        @PathVariable conversationId: UUID,
        @Valid @RequestBody request: ShareConversationRequest,
        authentication: Authentication,
    ): ResponseEntity<ConversationShareResponseVm> {
        val username = (authentication.principal as Jwt).subject
        val shareResponse = conversationShareService.shareConversation(conversationId, request, username)
        return ResponseEntity.ok(shareResponse)
    }
}
