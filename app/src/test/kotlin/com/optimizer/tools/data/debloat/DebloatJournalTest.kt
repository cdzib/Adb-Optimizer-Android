package com.optimizer.tools.data.debloat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DebloatJournalTest {
    @Test
    fun `registra desinstalaciones y las conserva entre instancias`() {
        val file = File.createTempFile("debloat-journal", ".json").also { it.delete() }
        try {
            val journal = DebloatJournal(file)

            assertEquals(emptySet<String>(), journal.uninstalled())
            journal.recordUninstall("com.example.a")
            journal.recordUninstall("com.example.b")
            journal.recordUninstall("com.example.a")

            assertEquals(setOf("com.example.a", "com.example.b"), DebloatJournal(file).uninstalled())

            journal.forget("com.example.a")

            assertEquals(setOf("com.example.b"), DebloatJournal(file).uninstalled())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `un archivo corrupto se trata como bitacora vacia`() {
        val file = File.createTempFile("debloat-journal", ".json")
        try {
            file.writeText("no es json")

            assertEquals(emptySet<String>(), DebloatJournal(file).uninstalled())
        } finally {
            file.delete()
        }
    }
}
