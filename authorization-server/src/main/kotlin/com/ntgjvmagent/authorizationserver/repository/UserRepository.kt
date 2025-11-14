package com.ntgjvmagent.authorizationserver.repository

import com.ntgjvmagent.authorizationserver.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface UserRepository :
    JpaRepository<UserEntity, String>,
    JpaSpecificationExecutor<UserEntity>


