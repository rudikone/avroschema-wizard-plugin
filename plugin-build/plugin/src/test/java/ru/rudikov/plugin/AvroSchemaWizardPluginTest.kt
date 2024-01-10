package ru.rudikov.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AvroSchemaWizardPluginTest {
    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        assert(project.tasks.getByName(REGISTER_TASK_NAME) is RegisterTask)
        assert(project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) is CompatibilityCheckTask)
    }

    @Test
    fun `extension avroWizardConfig is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        assertNotNull(project.extensions.getByName(EXTENSION_NAME))
    }

    @Test
    fun `parameters are passed correctly from extension to REGISTER_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val schemaRegistryUrl = "some_url"
        val searchAvroFilesPaths = setOf("some_path")
        val subjectToSchema = mapOf("some_subject" to "some_schema")

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.searchAvroFilesPaths.set(searchAvroFilesPaths)
            this.subjectToSchema.set(subjectToSchema)
        }

        val task = project.tasks.getByName(REGISTER_TASK_NAME) as RegisterTask

        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())
        assertEquals(searchAvroFilesPaths, task.searchAvroFilesPaths.get())
        assertEquals(subjectToSchema, task.subjectToSchema.get())
    }

    @Test
    fun `parameters are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val schemaRegistryUrl = "some_url"
        val searchAvroFilesPaths = setOf("some_path")
        val subjectToSchema = mapOf("some_subject" to "some_schema")

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.searchAvroFilesPaths.set(searchAvroFilesPaths)
            this.subjectToSchema.set(subjectToSchema)
        }

        val task = project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask

        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())
        assertEquals(searchAvroFilesPaths, task.searchAvroFilesPaths.get())
        assertEquals(subjectToSchema, task.subjectToSchema.get())
    }

    @Test
    fun `parameters by default are passed correctly from extension to REGISTER_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val subjectToSchema = mapOf("some_subject" to "some_schema")

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.subjectToSchema.set(subjectToSchema)
        }

        val task = project.tasks.getByName(REGISTER_TASK_NAME) as RegisterTask

        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())
        assertEquals(setOf(project.layout.buildDirectory.get().asFile.absolutePath), task.searchAvroFilesPaths.get())
    }

    @Test
    fun `parameters by default are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val subjectToSchema = mapOf("some_subject" to "some_schema")

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.subjectToSchema.set(subjectToSchema)
        }

        val task = project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask

        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())
        assertEquals(setOf(project.layout.buildDirectory.get().asFile.absolutePath), task.searchAvroFilesPaths.get())
    }

    companion object {
        private const val PLUGIN_ID = "ru.rudikov.avroschema-wizard-plugin"
    }
}
