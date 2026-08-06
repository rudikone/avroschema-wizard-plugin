package io.github.rudikone.avroschemawizardplugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AvroSchemaWizardPluginTest {
    private fun projectWithPlugin() =
        ProjectBuilder.builder().build().also {
            it.pluginManager.apply(PLUGIN_ID)
        }

    private fun extension(project: org.gradle.api.Project) =
        project
            .extensions
            .getByName(EXTENSION_NAME) as AvroWizardExtension

    private fun registerTask(project: org.gradle.api.Project) =
        project
            .tasks
            .getByName(REGISTER_TASK_NAME) as RegisterTask

    private fun checkTask(project: org.gradle.api.Project) =
        project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) as CompatibilityCheckTask

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = projectWithPlugin()
        assert(project.tasks.getByName(REGISTER_TASK_NAME) is RegisterTask)
        assert(project.tasks.getByName(COMPATIBILITY_CHECK_TASK_NAME) is CompatibilityCheckTask)
    }

    @Test
    fun `extension avroWizardConfig is created correctly`() {
        val project = projectWithPlugin()
        assertNotNull(project.extensions.getByName(EXTENSION_NAME))
    }

    @Test
    fun `parameters are passed correctly from extension to REGISTER_TASK`() {
        val project = projectWithPlugin()
        val schemaRegistryUrl = "some_url"
        val topic = "some_topic"
        val searchAvroFilePath = "some_path"
        val protocol = "some_protocol"
        val schema = "some_schema"
        val subjectNameStrategy = SubjectNameStrategies.RecordNameStrategy.name

        extension(project).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.configs {
                topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                    this.subjectNameStrategy.set(subjectNameStrategy)
                }
            }
        }

        val task = registerTask(project)
        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())

        val spec = task.subjectSpecs.get().single { it.topic == topic }
        assertEquals(topic, spec.topic)
        assertEquals(searchAvroFilePath, spec.searchAvroFilePath)
        assertEquals(protocol, spec.protocol)
        assertEquals(schema, spec.schema)
        assertEquals(subjectNameStrategy, spec.subjectNameStrategy)
    }

    @Test
    fun `parameters are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = projectWithPlugin()
        val schemaRegistryUrl = "some_url"
        val topic = "some_topic"
        val searchAvroFilePath = "some_path"
        val protocol = "some_protocol"
        val schema = "some_schema"
        val subjectNameStrategy = SubjectNameStrategies.RecordNameStrategy.name

        extension(project).apply {
            this.schemaRegistryUrl.set(schemaRegistryUrl)
            this.configs {
                topic(topic) {
                    this.searchAvroFilePath.set(searchAvroFilePath)
                    this.protocol.set(protocol)
                    this.schema.set(schema)
                    this.subjectNameStrategy.set(subjectNameStrategy)
                }
            }
        }

        val task = checkTask(project)
        assertEquals(schemaRegistryUrl, task.schemaRegistryUrl.get())

        val spec = task.subjectSpecs.get().single { it.topic == topic }
        assertEquals(topic, spec.topic)
        assertEquals(searchAvroFilePath, spec.searchAvroFilePath)
        assertEquals(protocol, spec.protocol)
        assertEquals(schema, spec.schema)
        assertEquals(subjectNameStrategy, spec.subjectNameStrategy)
    }

    @Test
    fun `parameters by default are passed correctly from extension to REGISTER_TASK`() {
        val project = projectWithPlugin()
        val topic = "some_topic"
        val protocol = "some_protocol"
        val schema = "some_schema"

        extension(project).configs {
            topic(topic) {
                this.protocol.set(protocol)
                this.schema.set(schema)
            }
        }

        val task = registerTask(project)
        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())

        val spec = task.subjectSpecs.get().single { it.topic == topic }
        assertEquals(
            project.layout.buildDirectory
                .get()
                .asFile.absolutePath,
            spec.searchAvroFilePath,
        )
        assertEquals(DEFAULT_SUBJECT_NAME_STRATEGY, spec.subjectNameStrategy)
    }

    @Test
    fun `should pass only schema parameter from extension to REGISTER_TASK`() {
        val project = projectWithPlugin()
        val topic = "some_topic"
        val schema = "some_schema"

        extension(project).configs {
            topic(topic) {
                this.schema.set(schema)
            }
        }

        val spec = registerTask(project).subjectSpecs.get().single { it.topic == topic }
        assertEquals(schema, spec.schema)
        assertNull(spec.protocol)
    }

    @Test
    fun `parameters by default are passed correctly from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = projectWithPlugin()
        val topic = "some_topic"
        val protocol = "some_protocol"
        val schema = "some_schema"

        extension(project).configs {
            topic(topic) {
                this.protocol.set(protocol)
                this.schema.set(schema)
            }
        }

        val task = checkTask(project)
        assertEquals(DEFAULT_SCHEMA_REGISTRY_URL, task.schemaRegistryUrl.get())

        val spec = task.subjectSpecs.get().single { it.topic == topic }
        assertEquals(
            project.layout.buildDirectory
                .get()
                .asFile.absolutePath,
            spec.searchAvroFilePath,
        )
        assertEquals(DEFAULT_SUBJECT_NAME_STRATEGY, spec.subjectNameStrategy)
    }

    @Test
    fun `should pass only schema parameter from extension to COMPATIBILITY_CHECK_TASK`() {
        val project = projectWithPlugin()
        val topic = "some_topic"
        val schema = "some_schema"

        extension(project).configs {
            topic(topic) {
                this.schema.set(schema)
            }
        }

        val spec = checkTask(project).subjectSpecs.get().single { it.topic == topic }
        assertEquals(schema, spec.schema)
        assertNull(spec.protocol)
    }

    companion object {
        private const val PLUGIN_ID = "io.github.rudikone.avroschema-wizard-plugin"
    }
}
