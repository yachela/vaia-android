package com.vaia.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.vaia.domain.model.ChecklistDocument
import com.vaia.domain.model.ChecklistItem
import com.vaia.domain.model.Document
import com.vaia.domain.model.DocumentProgress
import com.vaia.domain.model.TripDocumentChecklist
import com.vaia.domain.repository.DocumentRepository
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentChecklistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleItem1 = ChecklistItem(id = "item-1", name = "Pasaporte", isCompleted = true)
    private val sampleItem2 = ChecklistItem(id = "item-2", name = "Visa", isCompleted = false)
    private val sampleItem3 = ChecklistItem(id = "item-3", name = "Hotel", isCompleted = false)

    private val sampleChecklist = TripDocumentChecklist(
        id = "chk-1",
        tripId = "trip-1",
        items = listOf(sampleItem1, sampleItem2, sampleItem3)
    )

    private fun makeViewModel(
        documentRepo: DocumentRepository
    ) = DocumentChecklistViewModel(documentRepo, SavedStateHandle(mapOf("tripId" to "trip-1")))

    // ── init & loadChecklist ──────────────────────────────────────────────────

    @Test
    fun `init loads checklist and calculates progress on success`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("chk-1", state.checklist?.id)
        assertEquals(3, state.checklist?.items?.size)
        
        // Progress should be calculated locally (1 completed out of 3 total = 33%)
        val progress = state.checklist?.progress
        assertEquals(1, progress?.completed)
        assertEquals(3, progress?.total)
        assertEquals(33, progress?.percentage)
    }

    @Test
    fun `init loads checklist with empty items sets progress to null`() = runTest {
        val emptyChecklist = sampleChecklist.copy(items = emptyList())
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(emptyChecklist)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.checklist?.progress)
        assertTrue(state.checklist?.items.orEmpty().isEmpty())
    }

    @Test
    fun `loadChecklist sets error state on failure`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.failure(RuntimeException("Error al cargar checklist"))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Error al cargar checklist", state.error)
        assertNull(state.checklist)
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    fun `addItem appends new item on success`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            addChecklistItemResult = Result.success(ChecklistItem(id = "item-4", name = "Seguro", isCompleted = false))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.addItem("Seguro")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(4, state.checklist?.items?.size)
        assertEquals("item-4", state.checklist?.items?.last()?.id)
        assertEquals("Seguro", state.checklist?.items?.last()?.name)
    }

    @Test
    fun `addItem sets error state on failure`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            addChecklistItemResult = Result.failure(RuntimeException("Error al agregar"))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.addItem("Seguro")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Error al agregar", state.error)
    }

    // ── toggleItemComplete ────────────────────────────────────────────────────

    @Test
    fun `toggleItemComplete updates item and recalculates progress on success`() = runTest {
        // Toggle item-2 (Visa) from incomplete to complete
        val updatedItem2 = sampleItem2.copy(isCompleted = true)
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            toggleChecklistItemCompleteResult = Result.success(updatedItem2)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.toggleItemComplete("item-2", true)
        advanceUntilIdle()

        val state = vm.uiState.value
        val items = state.checklist?.items.orEmpty()
        assertTrue(items.first { it.id == "item-2" }.isCompleted)

        // Progress recalculation: 2 completed out of 3 total = 66%
        val progress = state.checklist?.progress
        assertEquals(2, progress?.completed)
        assertEquals(3, progress?.total)
        assertEquals(66, progress?.percentage)
    }

    @Test
    fun `toggleItemComplete sets error on failure`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            toggleChecklistItemCompleteResult = Result.failure(RuntimeException("Error al togglear"))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.toggleItemComplete("item-2", true)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Error al togglear", state.error)
        // Item state should remain unchanged (sampleItem2 is incomplete)
        val items = state.checklist?.items.orEmpty()
        assertFalse(items.first { it.id == "item-2" }.isCompleted)
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test
    fun `deleteItem removes item and recalculates progress on success`() = runTest {
        // Delete item-1 (completed item)
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            deleteChecklistItemResult = Result.success(Unit)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deleteItem("item-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        val items = state.checklist?.items.orEmpty()
        assertEquals(2, items.size)
        assertFalse(items.any { it.id == "item-1" })

        // Progress recalculation: 0 completed out of 2 total = 0%
        val progress = state.checklist?.progress
        assertEquals(0, progress?.completed)
        assertEquals(2, progress?.total)
        assertEquals(0, progress?.percentage)
    }

    @Test
    fun `deleteItem sets error on failure`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            deleteChecklistItemResult = Result.failure(RuntimeException("Error al eliminar"))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deleteItem("item-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Error al eliminar", state.error)
        assertEquals(3, state.checklist?.items?.size)
    }

    // ── uploadDocument ────────────────────────────────────────────────────────

    @Test
    fun `uploadDocument marks item complete and recalculates progress on success`() = runTest {
        val file = File("dummy.pdf")
        val doc = ChecklistDocument(
            id = "doc-1",
            checklistItemId = "item-2",
            fileName = "dummy.pdf",
            filePath = "/path/dummy.pdf",
            mimeType = "application/pdf",
            fileSize = 1024L,
            source = "local"
        )
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            uploadChecklistDocumentResult = Result.success(doc)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.uploadDocument("item-2", file)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isUploading)
        
        val item = state.checklist?.items.orEmpty().first { it.id == "item-2" }
        assertTrue(item.isCompleted)
        assertEquals("doc-1", item.document?.id)

        // Progress: 2 completed out of 3 total = 66%
        val progress = state.checklist?.progress
        assertEquals(2, progress?.completed)
        assertEquals(3, progress?.total)
        assertEquals(66, progress?.percentage)
    }

    @Test
    fun `uploadDocument sets error on failure`() = runTest {
        val file = File("dummy.pdf")
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            uploadChecklistDocumentResult = Result.failure(RuntimeException("Error al subir"))
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.uploadDocument("item-2", file)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isUploading)
        assertEquals("Error al subir", state.error)
    }

    // ── importFromGoogleDrive ─────────────────────────────────────────────────

    @Test
    fun `importFromGoogleDrive marks item complete and recalculates progress on success`() = runTest {
        val doc = ChecklistDocument(
            id = "doc-2",
            checklistItemId = "item-3",
            fileName = "hotel.pdf",
            filePath = "/drive/hotel.pdf",
            mimeType = "application/pdf",
            fileSize = 2048L,
            source = "google_drive"
        )
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            importFromGoogleDriveResult = Result.success(doc)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.importFromGoogleDrive("item-3", "driveId", "token")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isUploading)
        
        val item = state.checklist?.items.orEmpty().first { it.id == "item-3" }
        assertTrue(item.isCompleted)
        assertEquals("doc-2", item.document?.id)

        // Progress: 2 completed out of 3 total = 66%
        val progress = state.checklist?.progress
        assertEquals(2, progress?.completed)
        assertEquals(3, progress?.total)
        assertEquals(66, progress?.percentage)
    }

    // ── deleteDocument ────────────────────────────────────────────────────────

    @Test
    fun `deleteDocument marks item incomplete, removes document, and recalculates progress on success`() = runTest {
        // Prepare checklist where item-1 has a document and is completed
        val doc1 = ChecklistDocument(
            id = "doc-1",
            checklistItemId = "item-1",
            fileName = "pasaporte.pdf",
            filePath = "/path/pasaporte.pdf",
            mimeType = "application/pdf",
            fileSize = 512L,
            source = "local"
        )
        val itemWithDoc = sampleItem1.copy(document = doc1, isCompleted = true)
        val checklistWithDoc = sampleChecklist.copy(items = listOf(itemWithDoc, sampleItem2, sampleItem3))

        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(checklistWithDoc),
            deleteChecklistDocumentResult = Result.success(Unit)
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deleteDocument("doc-1", "item-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        val item = state.checklist?.items.orEmpty().first { it.id == "item-1" }
        assertFalse(item.isCompleted)
        assertNull(item.document)

        // Progress: 0 completed out of 3 total = 0%
        val progress = state.checklist?.progress
        assertEquals(0, progress?.completed)
        assertEquals(3, progress?.total)
        assertEquals(0, progress?.percentage)
    }

    // ── previewDocument ───────────────────────────────────────────────────────

    @Test
    fun `previewDocument returns URL on success`() = runTest {
        val repo = FakeDocumentRepository(
            getChecklistResult = Result.success(sampleChecklist),
            previewChecklistDocumentResult = Result.success("https://preview.url")
        )
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        var previewUrl: String? = null
        vm.previewDocument("doc-1") { url ->
            previewUrl = url
        }
        advanceUntilIdle()

        assertEquals("https://preview.url", previewUrl)
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeDocumentRepository(
        private val getChecklistResult: Result<TripDocumentChecklist> = Result.failure(NotImplementedError()),
        private val addChecklistItemResult: Result<ChecklistItem> = Result.failure(NotImplementedError()),
        private val toggleChecklistItemCompleteResult: Result<ChecklistItem> = Result.failure(NotImplementedError()),
        private val deleteChecklistItemResult: Result<Unit> = Result.failure(NotImplementedError()),
        private val uploadChecklistDocumentResult: Result<ChecklistDocument> = Result.failure(NotImplementedError()),
        private val importFromGoogleDriveResult: Result<ChecklistDocument> = Result.failure(NotImplementedError()),
        private val previewChecklistDocumentResult: Result<String> = Result.failure(NotImplementedError()),
        private val deleteChecklistDocumentResult: Result<Unit> = Result.failure(NotImplementedError())
    ) : DocumentRepository {

        override suspend fun getDocuments(tripId: String): Result<List<Document>> = Result.success(emptyList())
        
        override suspend fun uploadDocument(
            tripId: String,
            file: File,
            description: String?,
            category: String?
        ): Result<Document> = Result.failure(NotImplementedError())

        override suspend fun deleteDocument(documentId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getChecklist(tripId: String): Result<TripDocumentChecklist> = getChecklistResult

        override suspend fun addChecklistItem(tripId: String, name: String): Result<ChecklistItem> = addChecklistItemResult

        override suspend fun toggleChecklistItemComplete(
            itemId: String,
            isCompleted: Boolean
        ): Result<ChecklistItem> = toggleChecklistItemCompleteResult

        override suspend fun deleteChecklistItem(itemId: String): Result<Unit> = deleteChecklistItemResult

        override suspend fun uploadChecklistDocument(
            itemId: String,
            file: File
        ): Result<ChecklistDocument> = uploadChecklistDocumentResult

        override suspend fun importFromGoogleDrive(
            itemId: String,
            fileId: String,
            accessToken: String
        ): Result<ChecklistDocument> = importFromGoogleDriveResult

        override suspend fun previewChecklistDocument(documentId: String): Result<String> = previewChecklistDocumentResult

        override suspend fun deleteChecklistDocument(documentId: String): Result<Unit> = deleteChecklistDocumentResult
    }
}
