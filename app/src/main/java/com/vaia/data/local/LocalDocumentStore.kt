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
 * El archivo se guarda con el id del documento como nombre, de forma que la copia se puede
 * encontrar sin persistir la ruta en Room. La extensión es solo una pista para los visores: la
 * búsqueda ignora el mime type, porque depender de que el backend devuelva exactamente el mismo
 * valor en dos endpoints distintos dejaba archivos huérfanos e imposibles de encontrar.
 */
@Singleton
class LocalDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun directory(): File = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    /** La copia local del documento, o null si este dispositivo no la tiene. */
    fun find(documentId: String): File? =
        directory().listFiles()?.firstOrNull { it.isFile && it.nameWithoutExtension == documentId }

    /** Copia [source] al almacenamiento privado. Devuelve null si la copia falla. */
    fun save(documentId: String, source: File, mimeType: String): File? = try {
        // Una copia previa pudo quedar con otra extensión si cambió el mime type del documento.
        delete(documentId)
        source.copyTo(File(directory(), "$documentId.${extensionFor(mimeType)}"), overwrite = true)
    } catch (e: Exception) {
        ErrorLogger.log(
            feature = "documents",
            operation = "saveLocalCopy",
            throwable = e,
            metadata = mapOf("documentId" to documentId)
        )
        null
    }

    fun delete(documentId: String) {
        runCatching { find(documentId)?.delete() }
    }

    private fun extensionFor(mimeType: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.trim().lowercase())
            ?: DEFAULT_EXTENSION

    companion object {
        const val DIRECTORY = "documents"
        private const val DEFAULT_EXTENSION = "bin"
    }
}
