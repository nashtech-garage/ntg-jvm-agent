package com.ntgjvmagent.authorizationserver.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID


@Entity
@Table(name = "roles")
class RolesEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(unique = true, nullable = false)
    var name: String,
    @Column(length = 500)
    var description: String? = null,
    @OneToMany(mappedBy = "role")
    val userRoles: MutableSet<UserRolesEntity> = mutableSetOf(),
)
