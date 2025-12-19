package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.component.CurrentUserProvider
import com.ntgjvmagent.orchestrator.service.ConversationShareService
import com.ntgjvmagent.orchestrator.viewmodel.ConversationShareResponseVm
import com.ntgjvmagent.orchestrator.viewmodel.ShareConversationRequest
import com.ntgjvmagent.orchestrator.viewmodel.SharedConversationViewVm
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/share")
class SharedConversationController(
    private val conversationShareService: ConversationShareService,
    private val currentUserProvider: CurrentUserProvider,
) {
    /**
     * Get a shared conversation by share token (public endpoint - no authentication required)
     * @param shareToken Unique token for accessing the shared conversation
     * @return Shared conversation with messages
     */
    @GetMapping("/shared-conversations/{shareToken}")
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
     * @return Share response with share token and URL
     */
    @PostMapping("/shared-conversations/{conversationId}/share")
    fun shareConversation(
        @PathVariable conversationId: UUID,
        @Valid @RequestBody request: ShareConversationRequest,
    ): ResponseEntity<ConversationShareResponseVm> {
        val userId = currentUserProvider.getUserId()
        val shareResponse = conversationShareService.shareConversation(conversationId, request, userId)
        return ResponseEntity.ok(shareResponse)
    }
}
