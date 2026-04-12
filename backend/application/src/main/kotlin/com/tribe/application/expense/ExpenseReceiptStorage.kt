package com.tribe.application.expense

interface ExpenseReceiptStorage {
    fun upload(imageBytes: ByteArray, folder: String, mimeType: String): String
}
