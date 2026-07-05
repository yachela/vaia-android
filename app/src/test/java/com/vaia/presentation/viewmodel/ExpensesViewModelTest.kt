package com.vaia.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.vaia.domain.model.*
import com.vaia.domain.repository.*
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleExpense = Expense(
        id = "e1", amount = 50.0, description = "Cena", date = "2026-06-01", category = "food"
    )
    private val expenseWithReceipt = Expense(
        id = "e2", amount = 100.0, description = "Hotel", date = "2026-06-02",
        category = "accommodation", receiptImageUrl = "https://example.com/receipt.pdf"
    )

    private fun makeViewModel(
        repo: ExpenseRepository = FakeExpenseRepository(),
        tripRepo: TripRepository = FakeTripRepository()
    ) = ExpensesViewModel(repo, tripRepo, SavedStateHandle(mapOf("tripId" to "trip-1")))

    // ── loadExpenses ──────────────────────────────────────────────────────────

    @Test
    fun `init loads expenses on success`() = runTest {
        val repo = FakeExpenseRepository(getResult = Result.success(listOf(sampleExpense)))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        assertEquals(listOf(sampleExpense), vm.expenses.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `loadExpenses sets error on failure`() = runTest {
        val repo = FakeExpenseRepository(getResult = Result.failure(RuntimeException("sin red")))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        assertEquals("sin red", vm.error.value)
        assertTrue(vm.expenses.value.isEmpty())
    }

    // ── createExpense ─────────────────────────────────────────────────────────

    @Test
    fun `createExpense transitions to Success and reloads list`() = runTest {
        val repo = FakeExpenseRepository(createResult = Result.success(sampleExpense))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.createExpense("Cena", 50.0, "food", "2026-06-01")
        advanceUntilIdle()

        assertTrue(vm.createState.value is ExpensesViewModel.CreateState.Success)
        assertEquals(2, repo.getCalls) // init + reload after create
    }

    @Test
    fun `createExpense sets Error state on failure`() = runTest {
        val repo = FakeExpenseRepository(createResult = Result.failure(RuntimeException("error crear")))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.createExpense("X", 0.0, "other", "2026-06-01")
        advanceUntilIdle()

        val state = vm.createState.value
        assertTrue(state is ExpensesViewModel.CreateState.Error)
        assertEquals("error crear", (state as ExpensesViewModel.CreateState.Error).message)
    }

    // ── downloadReceipt ───────────────────────────────────────────────────────

    @Test
    fun `downloadReceipt sets Ready with bytes on success`() = runTest {
        val receiptBytes = byteArrayOf(1, 2, 3, 4)
        val repo = FakeExpenseRepository(receiptResult = Result.success(receiptBytes))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.downloadReceipt("e2")
        advanceUntilIdle()

        val state = vm.receiptState.value
        assertTrue(state is ExpensesViewModel.ReceiptState.Ready)
        assertTrue((state as ExpensesViewModel.ReceiptState.Ready).bytes.contentEquals(receiptBytes))
    }

    @Test
    fun `downloadReceipt sets Error state on failure`() = runTest {
        val repo = FakeExpenseRepository(receiptResult = Result.failure(RuntimeException("no encontrado")))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.downloadReceipt("e2")
        advanceUntilIdle()

        val state = vm.receiptState.value
        assertTrue(state is ExpensesViewModel.ReceiptState.Error)
        assertEquals("no encontrado", (state as ExpensesViewModel.ReceiptState.Error).message)
    }

    @Test
    fun `resetReceiptState resets to Idle`() = runTest {
        val repo = FakeExpenseRepository(receiptResult = Result.success(byteArrayOf(1)))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.downloadReceipt("e2")
        advanceUntilIdle()
        assertTrue(vm.receiptState.value is ExpensesViewModel.ReceiptState.Ready)

        vm.resetReceiptState()
        assertTrue(vm.receiptState.value is ExpensesViewModel.ReceiptState.Idle)
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    fun `deleteExpense transitions to Success`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.deleteExpense("e1")
        advanceUntilIdle()

        assertTrue(vm.deleteState.value is ExpensesViewModel.DeleteState.Success)
    }

    @Test
    fun `deleteExpense sets Error state on failure`() = runTest {
        val repo = FakeExpenseRepository(deleteResult = Result.failure(RuntimeException("no borrar")))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deleteExpense("e1")
        advanceUntilIdle()

        val state = vm.deleteState.value
        assertTrue(state is ExpensesViewModel.DeleteState.Error)
        assertEquals("no borrar", (state as ExpensesViewModel.DeleteState.Error).message)
    }

    // ── budgetAdvice ──────────────────────────────────────────────────────────

    @Test
    fun `init loads budget advice on success`() = runTest {
        val advice = BudgetAdvice("on_track", "Buen ritmo de gasto", 0.0, 0.0, 1000.0, 1, 5)
        val tripRepo = FakeTripRepository(budgetAdviceResult = Result.success(advice))
        val vm = makeViewModel(tripRepo = tripRepo)
        advanceUntilIdle()

        val state = vm.budgetAdviceState.value
        assertTrue(state is ExpensesViewModel.BudgetAdviceState.Success)
        assertEquals(advice, (state as ExpensesViewModel.BudgetAdviceState.Success).advice)
    }

    @Test
    fun `loadBudgetAdvice sets Error state on failure`() = runTest {
        val tripRepo = FakeTripRepository(budgetAdviceResult = Result.failure(RuntimeException("error ia")))
        val vm = makeViewModel(tripRepo = tripRepo)
        advanceUntilIdle()

        val state = vm.budgetAdviceState.value
        assertTrue(state is ExpensesViewModel.BudgetAdviceState.Error)
        assertEquals("No se pudo cargar el consejo de presupuesto.", (state as ExpensesViewModel.BudgetAdviceState.Error).message)
    }

    // ── Fake ──────────────────────────────────────────────────────────────────

    private class FakeTripRepository(
        private val budgetAdviceResult: Result<BudgetAdvice> = Result.success(
            BudgetAdvice("on_track", "Buen ritmo de gasto", 0.0, 0.0, 1000.0, 1, 5)
        )
    ) : TripRepository {
        var getBudgetAdviceCalls = 0

        override suspend fun getTrips(): Result<List<Trip>> = Result.success(emptyList())
        override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> = Result.success(Pair(emptyList(), false))
        override suspend fun getTrip(tripId: String): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
        override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> = Result.failure(NotImplementedError())
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = Result.failure(NotImplementedError())
        override suspend fun getBudgetAdvice(tripId: String): Result<BudgetAdvice> {
            getBudgetAdviceCalls++
            return budgetAdviceResult
        }
    }

    private class FakeExpenseRepository(
        private val getResult: Result<List<Expense>> = Result.success(emptyList()),
        private val createResult: Result<Expense> = Result.success(
            Expense("e1", 0.0, "X", "2026-06-01", "other")
        ),
        private val updateResult: Result<Expense> = Result.success(
            Expense("e1", 0.0, "X", "2026-06-01", "other")
        ),
        private val deleteResult: Result<Unit> = Result.success(Unit),
        private val receiptResult: Result<ByteArray> = Result.success(ByteArray(0))
    ) : ExpenseRepository {

        var getCalls = 0

        override suspend fun getExpenses(tripId: String): Result<List<Expense>> {
            getCalls++
            return getResult
        }

        override suspend fun getExpense(tripId: String, expenseId: String): Result<Expense> =
            Result.failure(NotImplementedError())

        override suspend fun createExpense(
            tripId: String, amount: Double, description: String,
            date: String, category: String, receiptImage: ByteArray?
        ): Result<Expense> = createResult

        override suspend fun updateExpense(
            tripId: String, expenseId: String, amount: Double, description: String,
            date: String, category: String, receiptImage: ByteArray?
        ): Result<Expense> = updateResult

        override suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit> = deleteResult

        override suspend fun downloadReceipt(tripId: String, expenseId: String): Result<ByteArray> = receiptResult
    }
}
