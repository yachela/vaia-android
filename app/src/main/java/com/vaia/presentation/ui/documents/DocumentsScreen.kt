package com.vaia.presentation.ui.documents

import androidx.compose.ui.res.stringResource
import com.vaia.R

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaia.di.AppContainer
import com.vaia.domain.model.Document
import com.vaia.presentation.viewmodel.DocumentsViewModel
import com.vaia.presentation.viewmodel.DocumentsViewModelFactory
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    appContainer: AppContainer,
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
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
            FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_document))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.documents) { document ->
                    DocumentItem(
                        document = document,
                        onDeleteClick = { documentsViewModel.deleteDocument(document.id) }
                    )
                }
            }

            if (showUploadDialog) {
                AlertDialog(
                    onDismissRequest = { showUploadDialog = false },
                    title = { Text(stringResource(R.string.upload_document)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.selected_file, selectedFileUri?.lastPathSegment ?: "N/A"))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = documentDescription,
                                onValueChange = { documentDescription = it },
                                label = { Text(stringResource(R.string.description_optional)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                selectedFileUri?.let { uri ->
                                    val tempFile = getFileFromUri(context, uri)
                                    if (tempFile != null) {
                                        documentsViewModel.uploadDocument(tempFile, documentDescription)
                                    }
                                }
                                showUploadDialog = false
                                documentDescription = ""
                            },
                            enabled = selectedFileUri != null && !uiState.isUploading
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text(stringResource(R.string.upload))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUploadDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DocumentItem(document: Document, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = document.fileName, style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.document_size, document.fileSize / 1024), style = MaterialTheme.typography.bodySmall)
                document.description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_document))
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
