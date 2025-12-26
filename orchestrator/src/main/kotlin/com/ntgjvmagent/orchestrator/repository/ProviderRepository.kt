package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.dto.response.ProviderNameResponseDto
import com.ntgjvmagent.orchestrator.entity.provider.Provider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProviderRepository : JpaRepository<Provider, UUID> {
    @Query(
        """
        SELECT
            p.id, p.name, p.active
        FROM Provider p
        WHERE p.active = true
        ORDER BY p.name ASC
    """,
        nativeQuery = true,
    )
    fun getProviderNameByActiveTrue(): List<ProviderNameResponseDto>
}
