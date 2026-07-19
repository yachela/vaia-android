package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.ExpenseDao
import com.vaia.data.local.db.toEntity
import com.vaia.data.local.db.toExpense
import com.vaia.domain.model.Expense
import com.vaia.domain.repository.ExpenseRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.vaia.data.local.ErrorLogger
import org.json.JSONObject
import java.io.ByteArrayInputStream

class ExpenseRepositoryImpl(
    private val apiService: VaiaApiService,
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override suspend fun getExpenses(tripId: String): Result<List<Expense>> {
        return try {
            val response = apiService.getExpenses(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expenses ->
                    expenseDao.deleteByTripId(tripId)
                    expenseDao.insertAll(expenses.map { it.toEntity(tripId) })
                    Result.success(expenses)
                } ?: Result.failure(Exception("No expenses data received"))
            } else {
                cachedExpenses(tripId)?.let { return Result.success(it) }
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudieron obtener los gastos: $errorMessage"))
            }
        } catch (e: Exception) {
            cachedExpenses(tripId)?.let { return Result.success(it) }
            Result.failure(ErrorLogger.logAndWrap("Expense", "getExpenses", e, "No se pudieron obtener los gastos"))
        }
    }

    /** Gastos guardados del viaje, o null si no hay nada cacheado. */
    private suspend fun cachedExpenses(tripId: String): List<Expense>? =
        expenseDao.getByTripId(tripId).takeIf { it.isNotEmpty() }?.map { it.toExpense() }

    override suspend fun getExpense(tripId: String, expenseId: String): Result<Expense> {
        return try {
            val response = apiService.getExpense(tripId, expenseId)
            if (response.isSuccessful) {
                response.body()?.data?.let { expense ->
                    expenseDao.insert(expense.toEntity(tripId))
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                expenseDao.getById(expenseId)?.let { return Result.success(it.toExpense()) }
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo obtener el gasto: $errorMessage"))
            }
        } catch (e: Exception) {
            expenseDao.getById(expenseId)?.let { return Result.success(it.toExpense()) }
            Result.failure(ErrorLogger.logAndWrap("Expense", "getExpense", e, "No se pudo obtener el gasto"))
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
                    expenseDao.insert(expense.toEntity(tripId))
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo crear el gasto: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Expense", "createExpense", e, "No se pudo crear el gasto"))
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
                    expenseDao.insert(expense.toEntity(tripId))
                    Result.success(expense)
                } ?: Result.failure(Exception("No expense data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo actualizar el gasto: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Expense", "updateExpense", e, "No se pudo actualizar el gasto"))
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
                Result.failure(Exception("Error al descargar recibo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Expense", "downloadReceipt", e, "No se pudo descargar el recibo"))
        }
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> {
        return try {
            val response = apiService.deleteExpense(tripId, expenseId)
            if (response.isSuccessful) {
                expenseDao.deleteById(expenseId)
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo eliminar el gasto: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Expense", "deleteExpense", e, "No se pudo eliminar el gasto"))
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
