package com.ntgjvmagent.orchestrator.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Table
import jakarta.persistence.ManyToMany

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
        joinColumns = [JoinColumn(name = "username")],
    )
    @Column(name = "authority")
    val roles: Set<String> = setOf("ROLE_USER"),
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "username")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var userRoles: MutableSet<Role> = mutableSetOf()
)
