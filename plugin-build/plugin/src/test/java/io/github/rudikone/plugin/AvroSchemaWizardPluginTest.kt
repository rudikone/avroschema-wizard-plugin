package io.github.rudikone.plugin

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
        val topic = "some_topic"
        val searchAvroFilePath = "some_path"
        val protocol = "some_protocol"
        val schema = "some_schema"
        val subjectNameStrategy = SubjectNameStrategies.RecordNameStrategy.name

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.configs {
                it.topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                    this.subjectNameStrategy.set(subjectNameStrategy)
                }
            }
        }

        val task = project.tasks.getByName(REGISTER_TASK_NAME) as RegisterTask

        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())

        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(topic, subjectConfig!!.name)
        assertEquals(searchAvroFilePath, subjectConfig.searchAvroFilePath.get())
        assertEquals(protocol, subjectConfig.protocol.get())
        assertEquals(schema, subjectConfig.schema.get())
        assertEquals(subjectNameStrategy, subjectConfig.subjectNameStrategy.get())
    }

    @Test
    fun `parameters are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val schemaRegistryUrl = "some_url"
        val topic = "some_topic"
        val searchAvroFilePath = "some_path"
        val protocol = "some_protocol"
        val schema = "some_schema"
        val subjectNameStrategy = SubjectNameStrategies.RecordNameStrategy.name

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.configs {
                it.topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                    this.subjectNameStrategy.set(subjectNameStrategy)
                }
            }
        }

        val task = project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask

        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())

        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(topic, subjectConfig!!.name)
        assertEquals(searchAvroFilePath, subjectConfig.searchAvroFilePath.get())
        assertEquals(protocol, subjectConfig.protocol.get())
        assertEquals(schema, subjectConfig.schema.get())
        assertEquals(subjectNameStrategy, subjectConfig.subjectNameStrategy.get())
    }

    @Test
    fun `parameters by default are passed correctly from extension to REGISTER_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val topic = "some_topic"
        val protocol = "some_protocol"
        val schema = "some_schema"

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.configs {
                it.topic(topic) {
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                }
            }
        }

        val task = project.tasks.getByName(REGISTER_TASK_NAME) as RegisterTask

        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())

        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(project.layout.buildDirectory.get().asFile.absolutePath, subjectConfig!!.searchAvroFilePath.get())
        assertEquals(DEFAULT_SUBJECT_NAME_STRATEGY, subjectConfig.subjectNameStrategy.get())
    }

    @Test
    fun `should passed only schema parameter from extension to REGISTER_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val topic = "some_topic"
        val schema = "some_schema"

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.configs {
                it.topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.schema.set(schema)
                }
            }
        }

        val task = project.tasks.getByName(REGISTER_TASK_NAME) as RegisterTask
        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(schema, subjectConfig!!.schema.get())
        assertEquals(null, subjectConfig.protocol.orNull)
    }

    @Test
    fun `parameters by default are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val topic = "some_topic"
        val protocol = "some_protocol"
        val schema = "some_schema"

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.configs {
                it.topic(topic) {
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                }
            }
        }

        val task = project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask

        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())

        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(project.layout.buildDirectory.get().asFile.absolutePath, subjectConfig!!.searchAvroFilePath.get())
        assertEquals(DEFAULT_SUBJECT_NAME_STRATEGY, subjectConfig.subjectNameStrategy.get())
    }

    @Test
    fun `should passed only schema parameter from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PLUGIN_ID)

        val topic = "some_topic"
        val schema = "some_schema"

        (project.extensions.getByName(EXTENSION_NAME) as AvroWizardExtension).apply {
            this.configs {
                it.topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.schema.set(schema)
                }
            }
        }

        val task = project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask
        val subjectConfig = task.subjectConfigs.get()[topic]

        assertEquals(schema, subjectConfig!!.schema.get())
        assertEquals(null, subjectConfig.protocol.orNull)
    }

    companion object {
        private const val PLUGIN_ID = "io.github.rudikone.avroschema-wizard-plugin"
    }
}
