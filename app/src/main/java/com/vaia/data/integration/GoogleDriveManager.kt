package com.vaia.data.integration

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.vaia.R
import com.vaia.data.local.AppException
import com.vaia.data.local.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class GoogleDriveManager(private val context: Context) {

    private var googleSignInClient: GoogleSignInClient? = null
    private var driveService: Drive? = null

    fun initialize() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient?.signInIntent
            ?: throw IllegalStateException("GoogleSignInClient no inicializado")
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            // getResult(ApiException::class) expone el statusCode real; `task.result` a secas
            // envolvía el fallo en una RuntimeExecutionException y perdíamos el motivo.
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)

            if (!hasDriveAccess(account)) {
                return userFacingFailure(
                    operation = "handleSignInResult",
                    throwable = IllegalStateException("Drive scope no concedido"),
                    userMessage = R.string.drive_error_missing_permission
                )
            }
            Result.success(account)
        } catch (e: ApiException) {
            userFacingFailure(
                operation = "handleSignInResult",
                throwable = e,
                userMessage = signInErrorMessage(e.statusCode),
                metadata = mapOf("statusCode" to e.statusCode)
            )
        } catch (e: Exception) {
            userFacingFailure(
                operation = "handleSignInResult",
                throwable = e,
                userMessage = R.string.drive_error_generic
            )
        }
    }

    /**
     * El detalle técnico (statusCode, stack trace, id de error) queda en el log; a la pantalla
     * solo llega una explicación en castellano de qué pasó y qué puede hacer el usuario.
     */
    private fun <T> userFacingFailure(
        operation: String,
        throwable: Throwable,
        @StringRes userMessage: Int,
        metadata: Map<String, Any?> = emptyMap()
    ): Result<T> {
        ErrorLogger.log(
            feature = "drive",
            operation = operation,
            throwable = throwable,
            metadata = metadata
        )
        return Result.failure(AppException.Api(context.getString(userMessage), throwable))
    }

    /**
     * Igual que [userFacingFailure], pero si la causa ya es un error nuestro con un mensaje
     * pensado para el usuario (típicamente el que propaga buildDriveService), lo deja pasar
     * en vez de taparlo con uno más genérico.
     */
    private fun <T> propagateFailure(
        operation: String,
        throwable: Throwable,
        @StringRes fallbackMessage: Int,
        metadata: Map<String, Any?> = emptyMap()
    ): Result<T> {
        if (throwable is AppException) {
            ErrorLogger.log(feature = "drive", operation = operation, throwable = throwable, metadata = metadata)
            return Result.failure(throwable)
        }
        return userFacingFailure(operation, throwable, fallbackMessage, metadata)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Tener una cuenta logueada no alcanza: puede venir de un login previo sin Drive, o el
     * usuario pudo rechazar el consentimiento. Sin esta verificación las llamadas a la API
     * fallaban recién en el servidor, con un 403 opaco.
     */
    private fun hasDriveAccess(account: GoogleSignInAccount?): Boolean =
        account != null && GoogleSignIn.hasPermissions(account, driveScope)

    suspend fun buildDriveService(): Result<Drive> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(
                    AppException.Api(context.getString(R.string.drive_error_no_account))
                )
            if (!hasDriveAccess(account)) {
                return@withContext Result.failure(
                    AppException.Api(context.getString(R.string.drive_error_missing_permission))
                )
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)
            ).setSelectedAccount(account.account)

            val drive = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("VAIA Travel App")
                .build()

            driveService = drive
            Result.success(drive)
        } catch (e: Exception) {
            userFacingFailure("buildDriveService", e, R.string.drive_error_generic)
        }
    }

    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        hasDriveAccess(getSignedInAccount())
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            googleSignInClient?.signOut()?.await()
            driveService = null
            Result.success(Unit)
        } catch (e: Exception) {
            userFacingFailure("signOut", e, R.string.drive_error_generic)
        }
    }

    suspend fun downloadFile(fileId: String, destinationFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: buildDriveService().getOrThrow()

            val outputStream = java.io.FileOutputStream(destinationFile)
            drive.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Result.success(Unit)
        } catch (e: Exception) {
            propagateFailure(
                operation = "downloadFile",
                throwable = e,
                fallbackMessage = R.string.drive_error_download,
                metadata = mapOf("fileId" to fileId)
            )
        }
    }

    suspend fun listFiles(folderId: String? = null): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: buildDriveService().getOrThrow()

            // Solo PDF, imágenes, Word, Excel y texto: el resto no sirve como documento de viaje.
            val mimeFilter = IMPORTABLE_MIME_TYPES.joinToString(" or ") { "mimeType='$it'" }
            var query = "trashed = false and ($mimeFilter)"
            if (folderId != null) {
                query += " and '$folderId' in parents"
            }

            val files = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, size, webViewLink)")
                .execute()

            val driveFiles = files.files.map { file ->
                DriveFile(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    size = (file.size ?: 0).toLong(),
                    webViewLink = file.webViewLink
                )
            }

            Result.success(driveFiles)
        } catch (e: Exception) {
            propagateFailure(
                operation = "listFiles",
                throwable = e,
                fallbackMessage = R.string.drive_error_list_files,
                metadata = mapOf("folderId" to folderId)
            )
        }
    }

    companion object {
        // La app solo lee de Drive para importar documentos: no pedimos el scope completo.
        private val driveScope = Scope(DriveScopes.DRIVE_READONLY)

        private val IMPORTABLE_MIME_TYPES = listOf(
            "application/pdf",
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
        )

        @StringRes
        private fun signInErrorMessage(statusCode: Int): Int = when (statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> R.string.drive_error_cancelled
            // DEVELOPER_ERROR es un problema de configuración del proyecto de Google (la firma
            // del APK no está registrada). El usuario no puede hacer nada al respecto, así que
            // se le ofrece la alternativa que sí tiene: subir el archivo desde el dispositivo.
            CommonStatusCodes.DEVELOPER_ERROR -> R.string.drive_error_not_enabled
            CommonStatusCodes.NETWORK_ERROR -> R.string.drive_error_network
            else -> R.string.drive_error_generic
        }
    }
}

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val webViewLink: String?
)
