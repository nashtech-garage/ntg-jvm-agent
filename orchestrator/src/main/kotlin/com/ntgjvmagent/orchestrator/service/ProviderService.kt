package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.response.ProviderNameResponseDto
import com.ntgjvmagent.orchestrator.dto.response.ProviderResponseDto
import com.ntgjvmagent.orchestrator.mapper.ProviderMapper
import com.ntgjvmagent.orchestrator.repository.ProviderRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProviderService(
    private val providerRepo: ProviderRepository,
) {
    fun getActiveProviderNames(): List<ProviderNameResponseDto> = providerRepo.getProviderNameByActiveTrue()

    fun getById(id: UUID): ProviderResponseDto {
        val provider =
            providerRepo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Provider not found: $id")
        return ProviderMapper.toProviderResponseDto(provider)
    }
}
