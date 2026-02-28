package com.vaia.presentation.ui.documents

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaia.domain.model.ChecklistItem
import com.vaia.presentation.viewmodel.DocumentChecklistUiState
import com.vaia.presentation.viewmodel.DocumentChecklistViewModel
import com.vaia.presentation.viewmodel.DocumentChecklistViewModelFactory

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
    val uiState by viewModel.uiState.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showUploadOptions by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents: $tripTitle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadChecklist() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
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
                                Text("Dismiss")
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
                            text = "No checklist items. Tap + to add one.",
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
            onLocalUpload = { file ->
                viewModel.uploadDocument(itemId, file)
                showUploadOptions = null
            },
            onGoogleDriveImport = { fileId, accessToken ->
                viewModel.importFromGoogleDrive(itemId, fileId, accessToken)
                showUploadOptions = null
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
                    text = "Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completed/$total complete",
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
    onDeleteDocument: () -> Unit
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
                        text = "Default",
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
                IconButton(onClick = { /* Preview */ }) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = "Preview")
                }
                IconButton(onClick = onDeleteDocument) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Document")
                }
            } else {
                IconButton(onClick = onUploadClick) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Upload")
                }
            }

            if (!item.isDefault) {
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete \"${item.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
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
        title = { Text("Add Checklist Item") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UploadOptionsDialog(
    onDismiss: () -> Unit,
    onLocalUpload: (java.io.File) -> Unit,
    onGoogleDriveImport: (String, String) -> Unit
) {
    var showGoogleDriveInput by remember { mutableStateOf(false) }
    var googleFileId by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = java.io.File(it.path ?: "file")
            onLocalUpload(file)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Document") },
        text = {
            Column {
                if (!showGoogleDriveInput) {
                    Text("Choose upload source:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Local")
                        }
                        OutlinedButton(onClick = { showGoogleDriveInput = true }) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Drive")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = googleFileId,
                        onValueChange = { googleFileId = it },
                        label = { Text("Google Drive File ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("Access Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showGoogleDriveInput) {
                TextButton(
                    onClick = { onGoogleDriveImport(googleFileId, accessToken) },
                    enabled = googleFileId.isNotBlank() && accessToken.isNotBlank()
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
