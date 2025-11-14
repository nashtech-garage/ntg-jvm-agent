package com.ntgjvmagent.authorizationserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    val username: String,

    val password: String,

    val enabled: Boolean = true,

    val name: String,

    val email: String,

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    val userRoles: MutableSet<UserRolesEntity> = mutableSetOf(),
)

