package com.vaia.presentation.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.data.integration.DriveFile
import com.vaia.data.integration.GoogleDriveManager
import com.vaia.presentation.ui.theme.InkMuted
import com.vaia.presentation.ui.theme.MintPrimary
import kotlinx.coroutines.launch

@Composable
fun DriveImportDialog(
    googleDriveManager: GoogleDriveManager,
    onFileSelected: (DriveFile) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSignedIn by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Carga el listado propagando el error: antes un getOrElse { emptyList() } convertía
    // cualquier 401/403 en "no hay archivos" y el problema real quedaba invisible.
    suspend fun loadFiles() {
        googleDriveManager.buildDriveService().fold(
            onSuccess = {
                googleDriveManager.listFiles().fold(
                    onSuccess = {
                        files = it
                        error = null
                    },
                    onFailure = { throwable -> error = throwable.message }
                )
            },
            onFailure = { throwable -> error = throwable.message }
        )
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isLoading = true
            googleDriveManager.handleSignInResult(result.data).fold(
                onSuccess = {
                    isSignedIn = true
                    error = null
                    loadFiles()
                },
                onFailure = { throwable ->
                    isSignedIn = false
                    error = throwable.message
                }
            )
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            googleDriveManager.initialize()
            isSignedIn = googleDriveManager.isSignedIn()
            if (isSignedIn) {
                loadFiles()
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.upload_from_drive)) },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MintPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.drive_loading_files),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }
                }
                !isSignedIn && error == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = InkMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.connect_google_drive_account),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.drive_signin_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                files.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = InkMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.drive_no_files),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }
                }
                else -> {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.drive_search_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = InkMuted)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        if (filteredFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.drive_no_search_results, searchQuery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredFiles) { file ->
                                    DriveFileItem(
                                        file = file,
                                        onClick = { onFileSelected(file) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSignedIn || error != null) {
                Button(
                    enabled = !isLoading,
                    onClick = {
                        // Un error con la cuenta ya vinculada casi siempre es falta de permiso
                        // sobre Drive: hay que desvincularla para que Google vuelva a pedir
                        // el consentimiento en lugar de reusar la sesión sin scope.
                        scope.launch {
                            if (isSignedIn) {
                                googleDriveManager.signOut()
                                isSignedIn = false
                            }
                            googleDriveManager.initialize()
                            signInLauncher.launch(googleDriveManager.getSignInIntent())
                        }
                    }
                ) {
                    Text(stringResource(R.string.connect_drive))
                }
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
fun DriveFileItem(
    file: DriveFile,
    onClick: () -> Unit
) {
    val (icon, iconColor, bgColor) = getFileStyle(file.mimeType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class FileStyle(val icon: ImageVector, val iconColor: Color, val bgColor: Color)

fun getFileStyle(mimeType: String): FileStyle = when {
    mimeType == "application/pdf" ->
        FileStyle(Icons.Filled.Description, Color(0xFFE53935), Color(0xFFFFEBEE))
    mimeType.contains("image") ->
        FileStyle(Icons.Filled.Image, Color(0xFF039BE5), Color(0xFFE1F5FE))
    mimeType.contains("word") || mimeType == "application/msword" ->
        FileStyle(Icons.Filled.Description, Color(0xFF1565C0), Color(0xFFE3F2FD))
    mimeType.contains("excel") || mimeType.contains("spreadsheet") || mimeType == "application/vnd.ms-excel" ->
        FileStyle(Icons.Filled.TableChart, Color(0xFF2E7D32), Color(0xFFE8F5E9))
    mimeType == "text/plain" ->
        FileStyle(Icons.Filled.InsertDriveFile, Color(0xFF616161), Color(0xFFF5F5F5))
    else ->
        FileStyle(Icons.Filled.InsertDriveFile, Color(0xFF757575), Color(0xFFF5F5F5))
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
