package com.ntgjvmagent.authorizationserver.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID,

    val username: String,

    val password: String,

    val enabled: Boolean = true,

    val name: String,

    val email: String,

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    val userRoles: MutableSet<UserRolesEntity> = mutableSetOf(),
)

