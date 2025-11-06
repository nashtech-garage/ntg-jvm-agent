package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.RoleService
import com.ntgjvmagent.orchestrator.viewmodel.AssignRoleRequest
import com.ntgjvmagent.orchestrator.viewmodel.RoleRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.RoleResponseVm
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/roles")
class RoleController(
    private val roleService: RoleService,
) {
    @GetMapping
    fun listRoles(): ResponseEntity<List<RoleResponseVm>> = ResponseEntity.ok(roleService.listRoles())

    @PostMapping
    fun createRole(
        @Valid @RequestBody request: RoleRequestVm,
    ): ResponseEntity<RoleResponseVm> = ResponseEntity.ok(roleService.createRole(request))

    @PutMapping("/{roleId}")
    fun updateRole(
        @PathVariable roleId: UUID,
        @Valid @RequestBody request: RoleRequestVm,
    ): ResponseEntity<RoleResponseVm> = ResponseEntity.ok(roleService.updateRole(roleId, request))

    @DeleteMapping("/{roleId}")
    fun deleteRole(
        @PathVariable roleId: UUID,
    ): ResponseEntity<Void> {
        roleService.deleteRole(roleId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/assign")
    fun assignRoles(
        @Valid @RequestBody request: AssignRoleRequest,
    ): ResponseEntity<Void> {
        roleService.assignRolesToUser(request.username, request.roleNames)
        return ResponseEntity.noContent().build()
    }
}
