package com.ntgjvmagent.authorizationserver.entity

import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
@SoftDelete(columnName = "is_deleted")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val username: String,

    val password: String,

    val enabled: Boolean = true,

    val name: String,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val userRoles: MutableSet<UserRolesEntity> = mutableSetOf(),
)

