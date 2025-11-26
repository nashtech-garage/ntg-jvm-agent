package com.ntgjvmagent.authorizationserver.controller

import com.ntgjvmagent.authorizationserver.dto.UpdateUserRequestDto
import com.ntgjvmagent.authorizationserver.dto.UpdateUserResponseDto
import com.ntgjvmagent.authorizationserver.request.CreateUserRequest
import com.ntgjvmagent.authorizationserver.dto.CreateUserDto
import com.ntgjvmagent.authorizationserver.dto.UserPageDto
import com.ntgjvmagent.authorizationserver.service.UserService
import com.ntgjvmagent.authorizationserver.utils.Constant
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getUsers(
        @RequestParam(defaultValue = Constant.PAGE_NUMBER, required = false) page: Int,
        @RequestParam(defaultValue = Constant.PAGE_SIZE, required = false) size: Int
    ): ResponseEntity<UserPageDto> {
        return ResponseEntity.ok(userService.getUsers(page, size))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUser(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateUserRequestDto
    ): ResponseEntity<UpdateUserResponseDto?> {
        val updatedUser = userService.updateUser(id, request)
        return ResponseEntity.ok(updatedUser)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<CreateUserDto> {
        val response = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
