package com.tribe.application.expense

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class NoOpExpenseReceiptAnalyzer : ExpenseReceiptAnalyzer {
    override fun analyze(imageBytes: ByteArray, mimeType: String): ReceiptAnalysis {
        throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
    }
}
