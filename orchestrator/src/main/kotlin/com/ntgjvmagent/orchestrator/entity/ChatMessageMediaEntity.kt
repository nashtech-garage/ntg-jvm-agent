package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "chat_message_media")
data class ChatMessageMediaEntity(
    @Column(name = "file_name")
    val fileName: String,
    @Column(name = "content_type")
    val contentType: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    val chatMessage: ChatMessageEntity,
    @Lob
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "data", columnDefinition = "bytea")
    val data: ByteArray,
    @Column(name = "file_size")
    val fileSize: Long,
) : BaseEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatMessageMediaEntity

        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (chatMessage.id != other.chatMessage.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + chatMessage.id.hashCode()
        return result
    }
}
