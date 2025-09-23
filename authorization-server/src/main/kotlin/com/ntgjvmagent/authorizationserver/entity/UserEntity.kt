package com.ntgjvmagent.authorizationserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    val username: String,

    val password: String,

    val enabled: Boolean = true,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "authorities",
        joinColumns = [JoinColumn(name = "username")]
    )
    @Column(name = "authority")
    val roles: Set<String> = setOf("ROLE_USER")
)

