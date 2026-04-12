package com.tribe.application.expense

interface CreateExpenseUseCase {
    fun createExpense(command: ExpenseCommand.Create): ExpenseResult.Detail
}

interface ListExpensesUseCase {
    fun listExpenses(query: ExpenseQuery.ListByTrip): List<ExpenseResult.Summary>
}

interface GetExpenseDetailUseCase {
    fun getExpenseDetail(query: ExpenseQuery.GetDetail): ExpenseResult.Detail
}

interface UpdateExpenseUseCase {
    fun updateExpense(command: ExpenseCommand.Update): ExpenseResult.Detail
}

interface AssignExpenseParticipantsUseCase {
    fun assignParticipants(command: ExpenseCommand.AssignParticipants): ExpenseResult.Detail
}

interface ClearExpenseAssignmentsUseCase {
    fun clearAssignments(command: ExpenseCommand.ClearAssignments): ExpenseResult.Detail
}

interface DeleteExpenseUseCase {
    fun deleteExpense(command: ExpenseCommand.Delete)
}
