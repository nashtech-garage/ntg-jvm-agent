package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.utils.AuthType

class AuthenticationRequestDto(
    val type: AuthType = AuthType.NONE,
    val token: String? = null,
    val headerName: String? = null,
)
