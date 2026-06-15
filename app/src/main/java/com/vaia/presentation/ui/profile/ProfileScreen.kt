package com.vaia.presentation.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vaia.R
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.WaypathBadge
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.common.WaypathCard
import com.vaia.presentation.viewmodel.AuthViewModel
import com.vaia.presentation.viewmodel.CurrencyInfo
import com.vaia.presentation.viewmodel.CurrencyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateCurrency: () -> Unit = {},
    authViewModel: AuthViewModel,
    currencyViewModel: CurrencyViewModel
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()
    val profileState by authViewModel.profileState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val availableCurrencies by currencyViewModel.availableCurrencies.collectAsState()

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        authViewModel.uploadAvatar(bytes, mimeType)
    }

    LaunchedEffect(Unit) { authViewModel.loadCurrentUser() }

    LaunchedEffect(profileState) {
        if (profileState is AuthViewModel.ProfileState.Saved) {
            android.widget.Toast.makeText(context, "Perfil actualizado correctamente", android.widget.Toast.LENGTH_SHORT).show()
            showEditDialog = false
            authViewModel.resetProfileState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    IconButton(onClick = { authViewModel.loadCurrentUser() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
            ) {
                AppQuickBar(
                    currentRoute = "profile",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer, // TODO: Implement explore navigation
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = onNavigateCurrency
                )
            }
        },
        containerColor = Color.Transparent
    )
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            endY = 500f
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = 100.dp
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                if (profileState is AuthViewModel.ProfileState.Loading && user == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    WaypathCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(88.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                if (user?.avatarUrl != null) {
                                    AsyncImage(
                                        model = user!!.avatarUrl,
                                        contentDescription = stringResource(R.string.profile_title),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = stringResource(R.string.cd_profile_photo),
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .clickable { avatarPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Cambiar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
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

                Spacer(modifier = Modifier.height(10.dp))

                WaypathCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.dark_mode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = stringResource(R.string.dark_mode_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onThemeChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                WaypathButton(
                    text = stringResource(R.string.close_session),
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    "Vas a salir de tu cuenta. Podés volver a iniciar sesión cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                WaypathButton(
                    text = "Cerrar sesión",
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = user?.name.orEmpty(),
            currentBio = user?.bio.orEmpty(),
            currentCountry = user?.country.orEmpty(),
            currentLanguage = user?.language.orEmpty(),
            currentCurrency = user?.currency.orEmpty(),
            availableCurrencies = availableCurrencies, // 👈 3. Se la pasás al diálogo de edición
            isSaving = profileState is AuthViewModel.ProfileState.Saving,
            onDismiss = {
                showEditDialog = false
                authViewModel.resetProfileState()
            },
            onSave = { name, bio, country, language, currency ->
                authViewModel.updateProfile(
                    name = name,
                    bio = bio.ifBlank { null },
                    country = country.ifBlank { null },
                    language = language.ifBlank { null },
                    currency = currency.ifBlank { null }
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
    availableCurrencies: List<CurrencyInfo>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, country: String, language: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var country by remember { mutableStateOf(currentCountry) }
    var language by remember { mutableStateOf(currentLanguage) }
    var currency by remember { mutableStateOf(currentCurrency) }

    var countryExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.description)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    enabled = !isSaving
                )
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { if (!isSaving) countryExpanded = !countryExpanded }
                ) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = {},
                        readOnly = true, // Evita que el usuario escriba a mano
                        label = { Text(stringResource(R.string.profile_country)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        enabled = !isSaving,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        Countries.forEach { selectedCountry ->
                            DropdownMenuItem(
                                text = { Text(selectedCountry) },
                                onClick = {
                                    country = selectedCountry
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { if (!isSaving) languageExpanded = !languageExpanded }
                ) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {},
                        readOnly = true, // Evita que el usuario escriba a mano
                        label = { Text(stringResource(R.string.profile_language)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        enabled = !isSaving,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        Languages.forEach { selectedLang ->
                            DropdownMenuItem(
                                text = { Text(selectedLang) },
                                onClick = {
                                    language = selectedLang
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { if (!isSaving) currencyExpanded = !currencyExpanded }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.profile_currency)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        enabled = !isSaving,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        availableCurrencies.forEach { currencyInfo ->
                            DropdownMenuItem(
                                text = { Text("${currencyInfo.code} (${currencyInfo.countryName})") },
                                onClick = {
                                    currency = currencyInfo.code
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
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

val Languages = listOf(
    "English",
    "Español",
    "Chino",
    "Françes",
    "Português",
    "Aleman",
    "Arabe",
    "Japanese",
    "Italiano",
    "Russian"
)

val Countries = listOf(
    // --- AMÉRICA (35 países) ---
    "Antigua y Barbuda", "Argentina", "Bahamas", "Barbados", "Belice", "Bolivia", "Brasil",
    "Canadá", "Chile", "Colombia", "Costa Rica", "Cuba", "Dominica", "Ecuador", "El Salvador",
    "Estados Unidos", "Granada", "Guatemala", "Guyana", "Haití", "Honduras", "Jamaica",
    "México", "Nicaragua", "Panamá", "Paraguay", "Perú", "República Dominicana",
    "San Cristóbal y Nieves", "San Vicente y las Granadinas", "Santa Lucía", "Surinam",
    "Trinidad y Tobago", "Uruguay", "Venezuela",

    // --- EUROPA (15 países clave) ---
    "Alemania", "Austria", "Bélgica", "España", "Francia", "Grecia", "Irlanda",
    "Italia", "Países Bajos", "Portugal", "Reino Unido", "República Checa", "Rusia",
    "Suiza", "Turquía",

    // --- ASIA, OCEANÍA Y ÁFRICA (10 países clave) ---
    "Australia", "China", "Corea del Sur", "Egipto", "Emiratos Árabes Unidos",
    "India", "Israel", "Japón", "Nueva Zelanda", "Sudáfrica"
)