package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
class User(
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
    val roles: Set<String> = setOf("ROLE_USER"),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = true)
    var role: Role,
)
