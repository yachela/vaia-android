package com.vaia.data.repository

import com.vaia.data.local.db.ExpenseDao
import com.vaia.data.local.db.ExpenseEntity
import com.vaia.data.local.db.toEntity
import com.vaia.data.local.db.toExpense
import com.vaia.domain.model.Expense
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cubre el cache local de gastos agregado en F4.4. Antes de esto la pantalla
 * de gastos quedaba vacía sin conexión porque el repositorio iba siempre a la API.
 */
class ExpenseRepositoryOfflineTest {

    private val tripId = "trip-1"

    private val sampleExpense = Expense(
        id = "exp-1",
        amount = 1500.0,
        description = "Cena en Trastevere",
        date = "2026-06-01",
        category = "Comida"
    )

    private class FakeExpenseDao : ExpenseDao {
        private val stored = linkedMapOf<String, ExpenseEntity>()

        override suspend fun getByTripId(tripId: String): List<ExpenseEntity> =
            stored.values.filter { it.tripId == tripId }

        override suspend fun getById(id: String): ExpenseEntity? = stored[id]

        override suspend fun insertAll(expenses: List<ExpenseEntity>) {
            expenses.forEach { stored[it.id] = it }
        }

        override suspend fun insert(expense: ExpenseEntity) {
            stored[expense.id] = expense
        }

        override suspend fun deleteById(id: String) {
            stored.remove(id)
        }

        override suspend fun deleteByTripId(tripId: String) {
            stored.values.removeAll { it.tripId == tripId }
        }

        override suspend fun getPendingSync(): List<ExpenseEntity> =
            stored.values.filter { it.syncStatus == "pending" }

        override suspend fun updateSyncStatus(id: String, status: String) {
            stored[id]?.let { stored[id] = it.copy(syncStatus = status) }
        }
    }

    @Test
    fun `el mapeo de ida y vuelta conserva los datos del gasto`() {
        val restored = sampleExpense.toEntity(tripId).toExpense()

        assertEquals(sampleExpense.id, restored.id)
        assertEquals(sampleExpense.amount, restored.amount, 0.001)
        assertEquals(sampleExpense.description, restored.description)
        assertEquals(sampleExpense.date, restored.date)
        assertEquals(sampleExpense.category, restored.category)
    }

    @Test
    fun `los campos nulos del cache no rompen el modelo de dominio`() {
        val incomplete = ExpenseEntity(id = "exp-x", tripId = tripId)

        val restored = incomplete.toExpense()

        assertEquals(0.0, restored.amount, 0.001)
        assertEquals("", restored.description)
        assertEquals("", restored.category)
        assertNull(restored.receiptImageUrl)
    }

    @Test
    fun `el cache devuelve solo los gastos del viaje pedido`() = runTest {
        val dao = FakeExpenseDao()
        dao.insert(sampleExpense.toEntity(tripId))
        dao.insert(sampleExpense.copy(id = "exp-2").toEntity("otro-trip"))

        val result = dao.getByTripId(tripId)

        assertEquals(1, result.size)
        assertEquals("exp-1", result.first().id)
    }

    @Test
    fun `refrescar reemplaza los gastos cacheados del viaje`() = runTest {
        val dao = FakeExpenseDao()
        dao.insert(sampleExpense.toEntity(tripId))

        // Mismo flujo que getExpenses tras una respuesta exitosa
        dao.deleteByTripId(tripId)
        dao.insertAll(listOf(sampleExpense.copy(id = "exp-9", description = "Museo").toEntity(tripId)))

        val result = dao.getByTripId(tripId)

        assertEquals(1, result.size)
        assertEquals("Museo", result.first().description)
    }

    @Test
    fun `borrar un gasto lo saca del cache`() = runTest {
        val dao = FakeExpenseDao()
        dao.insert(sampleExpense.toEntity(tripId))

        dao.deleteById("exp-1")

        assertTrue(dao.getByTripId(tripId).isEmpty())
    }

    @Test
    fun `sin datos cacheados la consulta devuelve vacio`() = runTest {
        val dao = FakeExpenseDao()

        assertTrue(dao.getByTripId(tripId).isEmpty())
        assertNull(dao.getById("no-existe"))
    }
}
