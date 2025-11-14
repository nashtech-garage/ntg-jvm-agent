package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val username: String,
    val password: String,
    val enabled: Boolean = true,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "username")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var userRoles: MutableSet<RoleEntity> = mutableSetOf(),
)
