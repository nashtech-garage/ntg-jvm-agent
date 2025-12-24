package com.ntgjvmagent.authorizationserver.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
class CustomUserDetails
    @JsonCreator
    constructor(
    @JsonProperty("userId")
    val userId: UUID,

    @JsonProperty("username")
    private val username: String,

    @JsonProperty("password")
    private val password: String,

    @JsonProperty("name")
    private val name: String,

    @JsonProperty("authorities")
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getUsername() = username

    fun getName() = name

    override fun getPassword() = password

    override fun getAuthorities() = authorities

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = true
}
