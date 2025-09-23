package com.ntgjvmagent.authorizationserver.repository

import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, String>

