package com.vaia.data.local

import android.content.Context
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda una copia de cada documento del checklist en el almacenamiento privado de la app.
 *
 * Los archivos no se hostean para consumo del cliente: la vista previa siempre sale de esta
 * copia local, así que el usuario solo puede ver los documentos en el dispositivo donde los
 * subió. Si la copia no existe, la UI esconde la vista previa.
 *
 * El nombre del archivo se deriva del id del documento y de su mime type, de forma que la ruta
 * se puede reconstruir sin persistirla en Room.
 */
@Singleton
class LocalDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun directory(): File = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    /** Ruta determinística de la copia local, exista o no el archivo. */
    fun pathFor(documentId: String, mimeType: String): File =
        File(directory(), "$documentId.${extensionFor(mimeType)}")

    /** La copia local del documento, o null si este dispositivo no la tiene. */
    fun find(documentId: String, mimeType: String): File? =
        pathFor(documentId, mimeType).takeIf { it.isFile }

    /** Copia [source] al almacenamiento privado. Devuelve null si la copia falla. */
    fun save(documentId: String, source: File, mimeType: String): File? = try {
        source.copyTo(pathFor(documentId, mimeType), overwrite = true)
    } catch (e: Exception) {
        ErrorLogger.log(
            feature = "documents",
            operation = "saveLocalCopy",
            throwable = e,
            metadata = mapOf("documentId" to documentId)
        )
        null
    }

    fun delete(documentId: String, mimeType: String) {
        runCatching { pathFor(documentId, mimeType).delete() }
    }

    private fun extensionFor(mimeType: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.trim().lowercase())
            ?: DEFAULT_EXTENSION

    companion object {
        const val DIRECTORY = "documents"
        private const val DEFAULT_EXTENSION = "bin"
    }
}
