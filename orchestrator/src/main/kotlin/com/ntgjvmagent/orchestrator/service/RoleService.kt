package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.Role
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.repository.RoleRepository
import com.ntgjvmagent.orchestrator.repository.UserRepository
import com.ntgjvmagent.orchestrator.viewmodel.RoleRequestVm
import com.ntgjvmagent.orchestrator.viewmodel.RoleResponseVm
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
) {
    fun listRoles(): List<RoleResponseVm> = roleRepository.findAll().map { it.toResponseVm() }

    fun createRole(request: RoleRequestVm): RoleResponseVm {
        // ensure no duplicate role name
        require(!roleRepository.existsByName(request.name)) {
            "Role with name '${request.name}' already exists"
        }

        val saved = roleRepository.save(Role(name = request.name, description = request.description))
        return saved.toResponseVm()
    }

    fun updateRole(
        roleId: UUID,
        request: RoleRequestVm,
    ): RoleResponseVm {
        val role =
            roleRepository
                .findById(roleId)
                .orElseThrow { ResourceNotFoundException("Role with id $roleId not found") }
        // ensure no duplicate role name
        require(!roleRepository.existsByName(request.name)) {
            "Role with name '${request.name}' already exists"
        }

        role.name = request.name
        role.description = request.description

        return roleRepository.save(role).toResponseVm()
    }

    fun deleteRole(roleId: UUID) {
        if (!roleRepository.existsById(roleId)) {
            throw ResourceNotFoundException("Role with id $roleId not found")
        }
        roleRepository.deleteById(roleId)
    }

    @Transactional
    fun assignRolesToUser(
        username: String,
        roleNames: List<String>,
    ) {
        val user =
            userRepository
                .findByUsername(username)
                ?: throw ResourceNotFoundException("User with username $username not found")

        val roles = roleRepository.findByNameIn(roleNames)
        if (roles.isEmpty()) {
            throw ResourceNotFoundException("No roles found for given names: $roleNames")
        }

        user.userRoles.addAll(roles)
        userRepository.save(user)
    }

    // Use an explicit function that safely reads the Role id
    private fun Role.toResponseVm(): RoleResponseVm =
        RoleResponseVm(
            id = this.id,
            name = this.name,
            description = this.description,
        )
}
