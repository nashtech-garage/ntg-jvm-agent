package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.ConversationShareService
import com.ntgjvmagent.orchestrator.viewmodel.ConversationShareResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ShareConversationRequest
import com.ntgjvmagent.orchestrator.viewmodel.SharedConversationViewVm
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
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

    /**
     * List all shares for a specific conversation (requires authentication)
     * @param conversationId ID of the conversation
     * @param authentication Current user authentication
     * @return List of shares for this conversation
     */
    @GetMapping("/conversation/{conversationId}/shares")
    fun listConversationShares(
        @PathVariable conversationId: UUID,
        authentication: Authentication,
    ): ResponseEntity<List<ConversationShareResponseVm>> {
        val username = (authentication.principal as Jwt).subject
        val shares = conversationShareService.listSharesByConversation(conversationId, username)
        return ResponseEntity.ok(shares)
    }

    /**
     * Revoke a shared conversation link (requires authentication)
     * @param shareToken Token of the share to revoke
     * @param authentication Current user authentication
     * @return Updated share response with expired status
     */
    @DeleteMapping("/{shareToken}")
    fun revokeShare(
        @PathVariable shareToken: String,
        authentication: Authentication,
    ): ResponseEntity<ConversationShareResponseVm> {
        val username = (authentication.principal as Jwt).subject
        val revokedShare = conversationShareService.revokeShare(shareToken, username)
        return ResponseEntity.ok(revokedShare)
    }
}
