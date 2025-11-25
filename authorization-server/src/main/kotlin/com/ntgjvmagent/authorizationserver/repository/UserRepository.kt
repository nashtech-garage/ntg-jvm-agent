package com.ntgjvmagent.authorizationserver.repository

import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface UserRepository :
    JpaRepository<UserEntity, UUID>,
    JpaSpecificationExecutor<UserEntity> {
    @Query(
        """
        SELECT u
        FROM UserEntity u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role
        WHERE u.username = :username
    """,
    )
    fun findUserByUserName(
        @Param("username") username: String
    ): Optional<UserEntity>

    fun findByEmail(email: String): Optional<UserEntity>
}


