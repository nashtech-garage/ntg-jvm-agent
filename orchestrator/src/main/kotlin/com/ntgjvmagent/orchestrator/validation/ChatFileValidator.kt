package com.ntgjvmagent.orchestrator.validation

import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.utils.Constant
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ChatFileValidator : ConstraintValidator<ValidChatFile, ChatRequestDto> {
    override fun isValid(
        value: ChatRequestDto?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        if (value == null || value.files == null) return true
        val files = value.files
        var isValid = true

        for (file in files) {
            when {
                file.isEmpty -> {
                    addViolation(context, "File must not be empty")
                    isValid = false
                }

                file.contentType?.startsWith("image/") != true -> {
                    addViolation(context, "Only image files are allowed")
                    isValid = false
                }

                file.size > Constant.MAXIMUM_UPLOAD_FILE_SIZE -> {
                    addViolation(context, "File size must not exceed 5MB")
                    isValid = false
                }
            }
        }

        return isValid
    }

    private fun addViolation(
        context: ConstraintValidatorContext?,
        message: String,
    ) {
        context?.disableDefaultConstraintViolation()
        context
            ?.buildConstraintViolationWithTemplate(message)
            ?.addPropertyNode("files")
            ?.addConstraintViolation()
    }
}
