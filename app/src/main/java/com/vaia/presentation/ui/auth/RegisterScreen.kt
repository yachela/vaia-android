package com.vaia.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.presentation.ui.common.WaypathBadge
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.common.WaypathCard
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val registerState by viewModel.registerState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(registerState) {
        when (registerState) {
            is AuthViewModel.AuthState.Success -> {
                isLoading = false
                onRegisterSuccess()
            }
            is AuthViewModel.AuthState.Error -> {
                isLoading = false
                errorMessage = (registerState as AuthViewModel.AuthState.Error).message
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
        WaypathCard(modifier = Modifier.fillMaxWidth()) {
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
                    Text("🗺️", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                WaypathBadge(text = stringResource(R.string.vaia_badge))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { emailFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        .focusRequester(emailFocusRequester)
                        .moveFocusOnEnterOrTab(focusManager)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { confirmPasswordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .moveFocusOnEnterOrTab(focusManager)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isLoading && name.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && confirmPassword.isNotBlank() &&
                                password == confirmPassword
                            ) {
                                viewModel.register(name, email, password, confirmPassword)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmPasswordFocusRequester)
                        .moveFocusOnEnterOrTab(focusManager, isDoneField = true) {
                            if (!isLoading && name.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && confirmPassword.isNotBlank() &&
                                password == confirmPassword
                            ) {
                                viewModel.register(name, email, password, confirmPassword)
                            }
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                WaypathButton(
                    text = if (isLoading) stringResource(R.string.loading_login) else stringResource(R.string.register),
                    onClick = { viewModel.register(name, email, password, confirmPassword) },
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = !isLoading && name.isNotBlank() && email.isNotBlank() &&
                        password.isNotBlank() && confirmPassword.isNotBlank() &&
                        password == confirmPassword
                )

                Spacer(modifier = Modifier.height(10.dp))

                WaypathButton(
                    text = stringResource(R.string.login),
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.already_have_account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
