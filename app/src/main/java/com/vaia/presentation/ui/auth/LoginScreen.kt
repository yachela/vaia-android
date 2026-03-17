package com.vaia.presentation.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.di.AppContainer
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

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
    val focusManager = LocalFocusManager.current
    val loginState by viewModel.loginState.collectAsState()

    fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun doLogin() {
        val trimmedEmail = email.trim()
        when {
            !validateEmail(trimmedEmail) -> {
                errorMessage = "Correo electrónico inválido"
            }
            password.length < 8 -> {
                errorMessage = "La contraseña debe tener al menos 8 caracteres"
            }
            else -> {
                errorMessage = null
                viewModel.login(trimmedEmail, password)
            }
        }
    }

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
            else -> isLoading = false
        }
    }

    AuthCardLayout(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
        content = {
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
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                            doLogin()
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnEnterOrTab(focusManager, isDoneField = true) {
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                            doLogin()
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
                text = if (isLoading) stringResource(R.string.loading_login) else stringResource(R.string.login),
                onClick = { doLogin() },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            )

            Spacer(modifier = Modifier.height(10.dp))

            WaypathButton(
                text = stringResource(R.string.register),
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth(),
                primary = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            val scope = rememberCoroutineScope()
            
            OutlinedButton(
                onClick = {
                    scope.launch {
                        com.vaia.data.DemoMode.isEnabled = true
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Demo Mode")
            }
        }
    )
}
