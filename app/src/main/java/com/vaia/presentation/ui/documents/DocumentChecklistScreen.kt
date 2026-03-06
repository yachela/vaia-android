package com.vaia.presentation.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaia.BuildConfig
import com.vaia.R
import com.vaia.data.integration.GoogleDriveManager
import com.vaia.presentation.ui.common.AppExceptionDialog
import com.vaia.presentation.ui.common.AppExceptionMapper
import com.vaia.presentation.ui.common.AppExceptionUi
import com.vaia.presentation.ui.common.DriveImportDialog
import com.vaia.domain.model.ChecklistItem
import com.vaia.presentation.viewmodel.DocumentChecklistUiState
import com.vaia.presentation.viewmodel.DocumentChecklistViewModel
import com.vaia.presentation.viewmodel.DocumentChecklistViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentChecklistScreen(
    tripId: String,
    tripTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: DocumentChecklistViewModel = viewModel(
        factory = DocumentChecklistViewModelFactory(
            (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.vaia.VaiaApplication).appContainer.documentRepository,
            tripId
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleDriveManager = remember { GoogleDriveManager(context.applicationContext) }
    val uiState by viewModel.uiState.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showUploadOptions by remember { mutableStateOf<String?>(null) }
    var pendingLocalUploadItemId by remember { mutableStateOf<String?>(null) }
    var showDriveImportForItemId by remember { mutableStateOf<String?>(null) }
    var appException by remember { mutableStateOf<AppExceptionUi?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val itemId = pendingLocalUploadItemId
        if (uri != null && itemId != null) {
            optimizeUriForUpload(context, uri)?.let { file ->
                viewModel.uploadDocument(itemId, file)
            } ?: run {
                viewModel.setError("El archivo es demasiado grande para subirlo. Intenta con una imagen más liviana.")
            }
        }
        pendingLocalUploadItemId = null
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            appException = AppExceptionMapper.fromMessage(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.documents_with_trip, tripTitle)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadChecklist() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Card
            uiState.checklist?.progress?.let { progress ->
                ProgressCard(progress.completed, progress.total, progress.percentage)
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    }
                }
                uiState.checklist?.items?.isEmpty() == true -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_checklist_items),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.checklist?.items ?: emptyList(),
                            key = { it.id }
                        ) { item ->
                            ChecklistItemCard(
                                item = item,
                                onToggleComplete = { isCompleted ->
                                    viewModel.toggleItemComplete(item.id, isCompleted)
                                },
                                onUploadClick = { showUploadOptions = item.id },
                                onDeleteClick = { viewModel.deleteItem(item.id) },
                                onDeleteDocument = {
                                    item.document?.let { doc ->
                                        viewModel.deleteDocument(doc.id, item.id)
                                    }
                                },
                                onPreviewDocument = {
                                    item.document?.let { doc ->
                                        viewModel.previewDocument(doc.id) { previewUrl ->
                                            try {
                                                openPreviewUrl(context, previewUrl)
                                            } catch (e: Exception) {
                                                appException = AppExceptionMapper.fromMessage(
                                                    e.message ?: "No se pudo abrir la vista previa del documento"
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddChecklistItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name ->
                viewModel.addItem(name)
                showAddItemDialog = false
            }
        )
    }

    // Upload Options Dialog
    showUploadOptions?.let { itemId ->
        UploadOptionsDialog(
            onDismiss = { showUploadOptions = null },
            onLocalUpload = {
                pendingLocalUploadItemId = itemId
                filePicker.launch("*/*")
                showUploadOptions = null
            },
            onGoogleDriveImport = {
                showDriveImportForItemId = itemId
                showUploadOptions = null
            }
        )
    }

    showDriveImportForItemId?.let { itemId ->
        DriveImportDialog(
            googleDriveManager = googleDriveManager,
            onFileSelected = { driveFile ->
                scope.launch {
                    val destination = File(
                        context.cacheDir,
                        "drive_${driveFile.id}_${System.currentTimeMillis()}"
                    )
                    googleDriveManager.downloadFile(driveFile.id, destination)
                        .onSuccess {
                            val optimizedFile = optimizeDownloadedFileForUpload(
                                file = destination,
                                mimeType = driveFile.mimeType
                            )
                            if (optimizedFile != null) {
                                viewModel.uploadDocument(itemId, optimizedFile)
                            } else {
                                viewModel.setError("El archivo de Drive excede el tamaño permitido para subir.")
                            }
                            showDriveImportForItemId = null
                        }
                }
            },
            onDismiss = { showDriveImportForItemId = null }
        )
    }

    appException?.let { exception ->
        AppExceptionDialog(
            exception = exception,
            onDismiss = {
                appException = null
                viewModel.clearError()
            }
        )
    }
}

@Composable
fun ProgressCard(completed: Int, total: Int, percentage: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.progress_summary, completed, total),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = percentage / 100f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ChecklistItemCard(
    item: ChecklistItem,
    onToggleComplete: (Boolean) -> Unit,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteDocument: () -> Unit,
    onPreviewDocument: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onToggleComplete
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (item.isDefault) {
                    Text(
                        text = stringResource(R.string.default_item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.document?.let { doc ->
                    Text(
                        text = doc.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action buttons
            if (item.document != null) {
                IconButton(onClick = onPreviewDocument) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = stringResource(R.string.preview))
                }
                IconButton(onClick = onDeleteDocument) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_document))
                }
            } else {
                IconButton(onClick = onUploadClick) {
                    Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.upload))
                }
            }

            if (!item.isDefault) {
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_item))
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_item)) },
            text = { Text(stringResource(R.string.delete_item_confirmation, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AddChecklistItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_checklist_item)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add_item))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun UploadOptionsDialog(
    onDismiss: () -> Unit,
    onLocalUpload: () -> Unit,
    onGoogleDriveImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.upload_document_title)) },
        text = {
            Column {
                Text(stringResource(R.string.choose_upload_source))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onLocalUpload) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.local))
                    }
                    OutlinedButton(onClick = onGoogleDriveImport) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.google_drive))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private const val MAX_UPLOAD_BYTES = 2 * 1024 * 1024
private const val MAX_IMAGE_SIDE = 1280

private fun optimizeUriForUpload(context: android.content.Context, uri: Uri): File? {
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    val rawFile = uriToTempFile(context, uri) ?: return null
    if (!isImageCandidate(mimeType, rawFile)) {
        return if (rawFile.length() <= MAX_UPLOAD_BYTES) rawFile else null
    }
    return compressImageFileIfNeeded(rawFile)
}

private fun optimizeDownloadedFileForUpload(file: File, mimeType: String): File? {
    if (!isImageCandidate(mimeType, file)) {
        return if (file.length() <= MAX_UPLOAD_BYTES) file else null
    }
    return compressImageFileIfNeeded(file)
}

private fun isImageCandidate(mimeType: String, file: File): Boolean {
    if (mimeType.startsWith("image/")) return true
    val extension = file.extension.lowercase()
    if (extension in setOf("jpg", "jpeg", "png", "gif", "webp")) return true
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return options.outWidth > 0 && options.outHeight > 0
}

private fun compressImageFileIfNeeded(file: File): File? {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    val resized = resizeBitmap(bitmap, MAX_IMAGE_SIDE)
    var quality = 82
    val output = if (resized == bitmap) file else File(file.parentFile, "compressed_${file.nameWithoutExtension}.jpg")

    while (quality >= 30) {
        FileOutputStream(output).use { fos ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }
        if (output.length() <= MAX_UPLOAD_BYTES) {
            if (resized != bitmap) bitmap.recycle()
            if (resized != bitmap) resized.recycle()
            return output
        }
        quality -= 10
    }

    if (resized != bitmap) bitmap.recycle()
    if (resized != bitmap) resized.recycle()
    return null
}

private fun resizeBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val maxCurrent = maxOf(width, height)
    if (maxCurrent <= maxSide) return bitmap
    val ratio = maxSide.toFloat() / maxCurrent.toFloat()
    val targetWidth = (width * ratio).toInt()
    val targetHeight = (height * ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private fun uriToTempFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val destination = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destination
    } catch (_: Exception) {
        null
    }
}

private fun openPreviewUrl(context: android.content.Context, rawUrl: String) {
    val finalUrl = rawUrl.toAbsoluteUrl(BuildConfig.API_BASE_URL)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        throw IllegalStateException("No hay una aplicación disponible para abrir el documento")
    }
}

private fun String.toAbsoluteUrl(baseUrl: String): String {
    val trimmed = trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedPath = trimmed.removePrefix("/")
    return "$normalizedBase/$normalizedPath"
}
