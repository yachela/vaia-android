package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.Expense
import com.vaia.domain.repository.ExpenseRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream

class ExpenseRepositoryImpl(
    private val apiService: VaiaApiService
) : ExpenseRepository {

    override suspend fun getExpenses(tripId: String): Result<List<Expense>> {
        return try {
            val response = apiService.getExpenses(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expenses ->
                    Result.success(expenses)
                } ?: Result.failure(Exception("No expenses data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get expenses: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExpense(tripId: String, expenseId: String): Result<Expense> {
        return try {
            val response = apiService.getExpense(tripId, expenseId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expense ->
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get expense: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to create expense: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to update expense: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> {
        return try {
            val response = apiService.deleteExpense(tripId, expenseId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to delete expense: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseApiError(rawBody: String?, fallback: String): String {
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(rawBody)
            when {
                json.has("errors") -> {
                    val errors = json.optJSONObject("errors")
                    val firstField = errors?.keys()?.asSequence()?.firstOrNull()
                    val firstMessage = firstField?.let { key -> errors.optJSONArray(key)?.optString(0) }
                    firstMessage ?: json.optString("message", fallback)
                }
                json.has("message") -> json.optString("message", fallback)
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
