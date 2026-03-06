package com.vaia.presentation.ui.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaia.R
import com.vaia.domain.model.Document
import com.vaia.presentation.ui.common.DocumentCategoryItem
import com.vaia.presentation.ui.common.DocumentProgressCard
import com.vaia.presentation.ui.common.documentCategories
import com.vaia.presentation.ui.common.getCategoryLabel
import com.vaia.presentation.ui.theme.InkMuted
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.ui.theme.SurfaceWhite
import com.vaia.presentation.viewmodel.DocumentsViewModel
import com.vaia.presentation.viewmodel.DocumentsViewModelFactory
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    appContainer: com.vaia.di.AppContainer,
    tripId: String,
    onBack: () -> Unit
) {
    val documentsViewModel: DocumentsViewModel = viewModel(
        factory = DocumentsViewModelFactory(
            appContainer.getTripDocumentsUseCase,
            appContainer.uploadDocumentUseCase,
            appContainer.deleteDocumentUseCase,
            tripId
        )
    )
    val uiState by documentsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showUploadDialog by remember { mutableStateOf(false) }
    var documentDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        if (uri != null) {
            showUploadDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.documents)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { /* TODO: Google Drive picker */ },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Filled.CloudUpload,
                        contentDescription = stringResource(R.string.upload_from_drive),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = MintPrimary
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.add_document))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyBackground.copy(alpha = 0.5f))
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.documents.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DocumentProgressCard(
                            completedCategories = uiState.completedCategories.size,
                            totalCategories = documentCategories.size
                        )
                    }

                    item {
                        Text(
                            text = "Categorías",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(documentCategories) { category ->
                        DocumentCategoryItem(
                            category = category,
                            isCompleted = category in uiState.completedCategories,
                            onClick = { filePickerLauncher.launch("*/*") }
                        )
                    }

                    if (uiState.documents.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Documentos subidos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(uiState.documents) { document ->
                            DocumentItem(
                                document = document,
                                onDeleteClick = { documentsViewModel.deleteDocument(document.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                )
            }
        }

        if (showUploadDialog) {
            UploadDocumentDialog(
                fileName = selectedFileUri?.lastPathSegment ?: "",
                description = documentDescription,
                onDescriptionChange = { documentDescription = it },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                isUploading = uiState.isUploading,
                onUpload = {
                    selectedFileUri?.let { uri ->
                        val tempFile = getFileFromUri(context, uri)
                        if (tempFile != null) {
                            documentsViewModel.uploadDocument(tempFile, documentDescription, selectedCategory)
                        }
                    }
                    showUploadDialog = false
                    documentDescription = ""
                    selectedCategory = null
                    selectedFileUri = null
                },
                onDismiss = {
                    showUploadDialog = false
                    documentDescription = ""
                    selectedCategory = null
                    selectedFileUri = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentDialog(
    fileName: String,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    isUploading: Boolean,
    onUpload: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.upload_document)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { getCategoryLabel(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_optional)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        documentCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(getCategoryLabel(category)) },
                                onClick = {
                                    onCategoryChange(category)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpload,
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.upload))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DocumentItem(document: Document, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.document_size, (document.fileSize / 1024).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
                document.category?.let { category ->
                    Text(
                        text = getCategoryLabel(category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MintPrimary
                    )
                }
                document.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_document),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getFileFromUri(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = uri.lastPathSegment ?: return null
    val file = File(context.cacheDir, fileName)

    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
