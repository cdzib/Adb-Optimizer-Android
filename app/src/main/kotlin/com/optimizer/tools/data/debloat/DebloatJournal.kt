package com.optimizer.tools.data.debloat

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Paquetes que el debloater desinstalo para el usuario 0, para poder reinstalarlos desde
 * «Restaurar» aunque la app se haya reiniciado.
 *
 * Las apps congeladas no necesitan bitacora: su estado se lee del dispositivo con
 * `pm list packages -d`. Solo la desinstalacion por usuario pierde esa informacion.
 */
class DebloatJournal(
    private val file: File,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    private val serializer = ListSerializer(String.serializer())
    private val lock = Any()

    /** Paquetes desinstalados por el debloater que siguen sin reinstalarse. */
    fun uninstalled(): Set<String> = synchronized(lock) { read() }

    fun recordUninstall(packageName: String): Unit =
        synchronized(lock) {
            val current = read()
            if (packageName !in current) write(current + packageName)
        }

    /** Olvida el paquete cuando ya se reinstalo o se restauro. */
    fun forget(packageName: String) {
        synchronized(lock) {
            val current = read()
            if (packageName in current) write(current - packageName)
        }
    }

    private fun read(): Set<String> =
        runCatching { json.decodeFromString(serializer, file.readText()).toSet() }
            .getOrElse { emptySet() }

    private fun write(packages: Set<String>) {
        val parent = file.absoluteFile.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return
        val tmp = File.createTempFile("${file.name}.", ".tmp", parent)
        try {
            tmp.writeText(json.encodeToString(serializer, packages.sorted()))
            try {
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }
}
