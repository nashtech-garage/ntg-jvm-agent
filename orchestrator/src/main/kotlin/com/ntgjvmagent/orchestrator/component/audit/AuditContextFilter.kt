package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.model.AuthUser
import com.ntgjvmagent.orchestrator.utils.Utils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuditContextFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated) {
            val userId = Utils.currentUserId(authentication)
            val name = Utils.currentUserName(authentication)

            AuditContext.set(
                AuthUser(
                    id = userId,
                    name = name,
                ),
            )
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            AuditContext.clear()
        }
    }
}
