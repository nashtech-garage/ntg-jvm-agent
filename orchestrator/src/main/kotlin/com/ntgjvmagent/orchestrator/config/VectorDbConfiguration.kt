package com.ntgjvmagent.orchestrator.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(VectorDbDataSourceProperties::class)
class VectorDbConfiguration {
    @Bean
    @Qualifier("vectorDataSource")
    fun vectorDataSource(props: VectorDbDataSourceProperties): DataSource =
        DataSourceBuilder
            .create()
            .url(props.url)
            .username(props.username)
            .password(props.password)
            .build()

    @Bean
    @Qualifier("vectorJdbcTemplate")
    fun vectorJdbcTemplate(
        @Qualifier("vectorDataSource") dataSource: DataSource,
    ): JdbcTemplate = JdbcTemplate(dataSource)
}
