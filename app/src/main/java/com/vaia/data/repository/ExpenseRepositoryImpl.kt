package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.api.dto.toDomain
import com.vaia.data.network.ErrorMapper
import com.vaia.domain.model.Expense
import com.vaia.domain.repository.ExpenseRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream

class ExpenseRepositoryImpl(
    private val apiService: VaiaApiService
) : ExpenseRepository {

    override suspend fun getExpenses(tripId: String): Result<List<Expense>> {
        return try {
            val response = apiService.getExpenses(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expenses ->
                    Result.success(expenses.map { it.toDomain() })
                } ?: Result.failure(Exception("No expenses data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudieron obtener los gastos"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun getExpense(tripId: String, expenseId: String): Result<Expense> {
        return try {
            val response = apiService.getExpense(tripId, expenseId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expense ->
                    Result.success(expense.toDomain())
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo obtener el gasto"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun createExpense(tripId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense> {
        return try {
            val amountBody = amount.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = receiptImage?.let {
                val imageStream = ByteArrayInputStream(it)
                val imageBody = imageStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("receipt_image", "receipt.jpg", imageBody)
            }

            val response = apiService.createExpense(tripId, amountBody, descriptionBody, dateBody, categoryBody, imagePart)
            if (response.isSuccessful) {
                response.body()?.data?.let { expense ->
                    Result.success(expense.toDomain())
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo crear el gasto"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun updateExpense(tripId: String, expenseId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense> {
        return try {
            val amountBody = amount.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = receiptImage?.let {
                val imageStream = ByteArrayInputStream(it)
                val imageBody = imageStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("receipt_image", "receipt.jpg", imageBody)
            }

            val response = apiService.updateExpense(tripId, expenseId, amountBody, descriptionBody, dateBody, categoryBody, imagePart)
            if (response.isSuccessful) {
                response.body()?.data?.let { expense ->
                    Result.success(expense.toDomain())
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar el gasto"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun downloadReceipt(tripId: String, expenseId: String): Result<ByteArray> {
        return try {
            val response = apiService.downloadReceipt(tripId, expenseId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                    ?: return Result.failure(Exception("Recibo vacío"))
                Result.success(bytes)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo descargar el recibo"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> {
        return try {
            val response = apiService.deleteExpense(tripId, expenseId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo eliminar el gasto"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

}
