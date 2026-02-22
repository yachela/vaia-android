package com.vaia.presentation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.WaypathBadge
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.common.WaypathCard
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    viewModel: AuthViewModel
) {
    val user by viewModel.currentUser.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (user == null) viewModel.loadCurrentUser()
    }

    LaunchedEffect(profileState) {
        if (profileState is AuthViewModel.ProfileState.Saved) {
            showEditDialog = false
            viewModel.resetProfileState()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
        bottomBar = {
            AppQuickBar(
                currentRoute = "profile",
                onHome = onNavigateHome,
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SkyBackground.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (profileState is AuthViewModel.ProfileState.Loading && user == null) {
                CircularProgressIndicator()
                return@Column
            }

            WaypathCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(Color(0xFFBCEBC8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        user?.name ?: stringResource(R.string.profile_default_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        user?.email ?: stringResource(R.string.profile_default_email),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WaypathBadge(text = stringResource(R.string.profile_subtitle))
                    Spacer(modifier = Modifier.height(12.dp))
                    WaypathButton(
                        text = stringResource(R.string.profile_edit),
                        onClick = { showEditDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (profileState is AuthViewModel.ProfileState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = (profileState as AuthViewModel.ProfileState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_card_country),
                    value = user?.country.orDash()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_card_language),
                    value = user?.language.orDash()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            StatCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.profile_card_preferences),
                value = "${stringResource(R.string.profile_currency)}: ${user?.currency.orDash()}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.description),
                value = user?.bio.orDash()
            )
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = user?.name.orEmpty(),
            currentBio = user?.bio.orEmpty(),
            currentCountry = user?.country.orEmpty(),
            currentLanguage = user?.language.orEmpty(),
            currentCurrency = user?.currency.orEmpty(),
            isSaving = profileState is AuthViewModel.ProfileState.Saving,
            onDismiss = {
                showEditDialog = false
                viewModel.resetProfileState()
            },
            onSave = { name, bio, country, language, currency ->
                viewModel.updateProfile(
                    name = name,
                    bio = bio.ifBlank { null },
                    country = country.ifBlank { null },
                    language = language.ifBlank { null },
                    currency = currency.ifBlank { null },
                    avatarUrl = user?.avatarUrl
                )
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    currentCountry: String,
    currentLanguage: String,
    currentCurrency: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, country: String, language: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var country by remember { mutableStateOf(currentCountry) }
    var language by remember { mutableStateOf(currentLanguage) }
    var currency by remember { mutableStateOf(currentCurrency) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.description)) },
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(stringResource(R.string.profile_country)) },
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text(stringResource(R.string.profile_language)) },
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text(stringResource(R.string.profile_currency)) },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        },
        confirmButton = {
            WaypathButton(
                text = if (isSaving) stringResource(R.string.loading) else stringResource(R.string.save),
                onClick = { onSave(name.trim(), bio.trim(), country.trim(), language.trim(), currency.trim()) },
                enabled = !isSaving && name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    WaypathCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

private fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this
