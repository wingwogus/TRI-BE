package com.tribe.application.expense

import java.math.BigDecimal

interface ExpenseReceiptAnalyzer {
    fun analyze(imageBytes: ByteArray, mimeType: String): ReceiptAnalysis
}

data class ReceiptAnalysis(
    val totalAmount: BigDecimal,
    val items: List<ReceiptItem>,
    val subtotal: BigDecimal? = null,
    val tax: BigDecimal? = null,
    val tip: BigDecimal? = null,
    val discount: BigDecimal? = null,
)

data class ReceiptItem(
    val itemName: String,
    val price: BigDecimal,
)
