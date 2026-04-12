package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class NoOpExpenseReceiptStorage : ExpenseReceiptStorage {
    override fun upload(imageBytes: ByteArray, folder: String, mimeType: String): String {
        throw BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED)
    }
}
