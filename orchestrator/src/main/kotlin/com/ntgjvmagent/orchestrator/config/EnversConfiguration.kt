package com.ntgjvmagent.orchestrator.config

import org.springframework.context.annotation.Configuration

/**
 * Configuration for Hibernate Envers entity auditing.
 *
 * Envers automatically tracks changes to entities annotated with @Audited.
 * For each audited entity, it maintains:
 * - Complete revision history
 * - Who made changes (username and user ID)
 * - When changes were made (timestamp)
 * - What type of change (ADD, MOD, DEL)
 *
 * Configuration properties are set in application.properties:
 * - audit_table_suffix: Suffix for audit tables (default: _aud)
 * - revision_field_name: Name of revision field (default: rev)
 * - revision_type_field_name: Name of revision type field (default: revtype)
 * - store_data_at_delete: Whether to store entity state when deleted (default: true)
 *
 * Audit tables are created via Flyway migration V2016__create_envers_audit_tables.sql
 *
 * Usage:
 * 1. Add @Audited to entities you want to track
 * 2. Use @NotAudited on collections/relationships to avoid auditing cascade
 * 3. Query audit history via AuditService
 * 4. Access audit data via REST API at /api/v1/audit
 *
 * Audited entities:
 * - Agent
 * - Tool
 * - ConversationEntity
 * - SystemSettingEntity
 */
@Configuration
class EnversConfiguration {
    // Configuration is loaded from application.properties
    // This class serves as documentation and can be extended with custom beans if needed
}

