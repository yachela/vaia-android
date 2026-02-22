package com.vaia.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.vaia.R
import com.vaia.presentation.ui.common.WaypathBadge
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.common.WaypathCard
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.ui.theme.SalmonOrange
import com.vaia.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthViewModel.AuthState.Success -> {
                isLoading = false
                onLoginSuccess()
            }
            is AuthViewModel.AuthState.Error -> {
                isLoading = false
                errorMessage = (loginState as AuthViewModel.AuthState.Error).message
            }
            is AuthViewModel.AuthState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            else -> {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SkyBackground,
                        Color(0xFFEAF4EE)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        WaypathCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MintPrimary.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧭", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.welcome_to),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                WaypathBadge(text = stringResource(R.string.vaia_badge))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager),
                    shape = MaterialTheme.shapes.medium // Campos con curvas pronunciadas
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .moveFocusOnEnterOrTab(focusManager, isDoneField = true) {
                            if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            }
                        },
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaypathButton(
                    text = if (isLoading) stringResource(R.string.loading_login) else stringResource(R.string.login),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.login(email, password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                WaypathButton(
                    text = stringResource(R.string.create_account),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToRegister()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primary = false
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}
