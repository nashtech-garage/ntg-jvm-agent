package com.ntgjvmagent.orchestrator.component.audit

import com.ntgjvmagent.orchestrator.model.AuthUser

object AuditContext {
    private val current = ThreadLocal<AuthUser?>()

    fun set(user: AuthUser) = current.set(user)

    fun get(): AuthUser = current.get() ?: throw IllegalStateException("AuthUser not found in AuditContext")

    fun clear() = current.remove()
}
