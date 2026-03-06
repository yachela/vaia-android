package com.vaia.data.integration

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
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
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient?.signInIntent
            ?: throw IllegalStateException("GoogleSignInClient no inicializado")
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Result.success(task.result)
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "handleSignInResult",
                    throwable = e,
                    defaultMessage = "No se pudo iniciar sesión en Drive"
                )
            )
        }
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun buildDriveService(): Result<Drive> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("No signed in account"))

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
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
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "buildDriveService",
                    throwable = e,
                    defaultMessage = "No se pudo inicializar Drive"
                )
            )
        }
    }

    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        getSignedInAccount() != null
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            googleSignInClient?.signOut()?.await()
            driveService = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "signOut",
                    throwable = e,
                    defaultMessage = "No se pudo cerrar sesión de Drive"
                )
            )
        }
    }

    suspend fun uploadFile(
        fileName: String,
        file: File,
        mimeType: String,
        folderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: buildDriveService().getOrThrow()

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = folderId?.let { listOf(it) }
            }

            val mediaContent = FileContent(mimeType, file)

            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            Result.success(uploadedFile.id)
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "uploadFile",
                    throwable = e,
                    defaultMessage = "No se pudo subir el archivo a Drive",
                    metadata = mapOf("fileName" to fileName)
                )
            )
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
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "downloadFile",
                    throwable = e,
                    defaultMessage = "No se pudo descargar el archivo de Drive",
                    metadata = mapOf("fileId" to fileId)
                )
            )
        }
    }

    suspend fun listFiles(folderId: String? = null): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: buildDriveService().getOrThrow()

            var query = "trashed = false"
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
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "listFiles",
                    throwable = e,
                    defaultMessage = "No se pudieron listar archivos de Drive",
                    metadata = mapOf("folderId" to folderId)
                )
            )
        }
    }

    suspend fun deleteFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: buildDriveService().getOrThrow()
            drive.files().delete(fileId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "drive",
                    operation = "deleteFile",
                    throwable = e,
                    defaultMessage = "No se pudo eliminar el archivo de Drive",
                    metadata = mapOf("fileId" to fileId)
                )
            )
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
