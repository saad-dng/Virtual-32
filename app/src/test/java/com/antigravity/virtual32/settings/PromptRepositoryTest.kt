package com.antigravity.virtual32.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PromptRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: PromptRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear DataStore preferences file for isolated tests
        val dataStoreFile = File(context.filesDir, "datastore/prompts.preferences_pb")
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
        repository = PromptRepository(context)
    }

    @Test
    fun `default presets are loaded initially`() = runBlocking {
        val presets = repository.presetsFlow.first()
        assertEquals(4, presets.size)
        assertTrue(presets.any { it.id == "default" })
        assertTrue(presets.any { it.id == "mcq_e" })
        assertTrue(presets.any { it.id == "true_false" })
        assertTrue(presets.any { it.id == "careful_columns" })
    }

    @Test
    fun `true false preset has correct mapping`() {
        val tfPreset = repository.defaultPresets.find { it.id == "true_false" }
        assertNotNull(tfPreset)
        assertTrue(tfPreset!!.name.contains("T -> A, F -> B"))
    }

    @Test
    fun `custom preset can be saved and loaded`() = runBlocking {
        val custom = PromptPreset(id = "custom1", name = "My Custom", instruction = "Do something")
        repository.saveCustomPreset(custom)

        val presets = repository.presetsFlow.first()
        assertEquals(5, presets.size) // 4 defaults + 1 custom
        val loaded = presets.find { it.id == "custom1" }
        assertNotNull(loaded)
        assertEquals("My Custom", loaded?.name)
        assertEquals("Do something", loaded?.instruction)
    }

    @Test
    fun `built-in presets cannot be overwritten`() = runBlocking {
        val defaultPreset = repository.defaultPresets.first()
        val malicious = defaultPreset.copy(instruction = "Hacked instruction")
        repository.saveCustomPreset(malicious) // Should ignore because isBuiltIn is true

        val presets = repository.presetsFlow.first()
        val loaded = presets.find { it.id == defaultPreset.id }
        assertEquals(defaultPreset.instruction, loaded?.instruction)
    }

    @Test
    fun `custom preset can be deleted`() = runBlocking {
        val custom = PromptPreset(id = "custom2", name = "Temp", instruction = "Temp inst")
        repository.saveCustomPreset(custom)

        var presets = repository.presetsFlow.first()
        assertEquals(5, presets.size)

        repository.deleteCustomPreset("custom2")
        presets = repository.presetsFlow.first()
        assertEquals(4, presets.size) // Only defaults left
    }
}
