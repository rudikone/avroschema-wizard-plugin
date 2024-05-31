package io.github.rudikone.plugin

import io.github.rudikone.plugin.SubjectNameStrategies.TopicNameStrategy
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

const val DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:10081"
val DEFAULT_SUBJECT_NAME_STRATEGY = TopicNameStrategy.name

@Suppress("UnnecessaryAbstractClass")
abstract class AvroWizardExtension
    @Inject
    constructor(project: Project) {
        private val objects = project.objects

        val schemaRegistryUrl: Property<String> =
            objects.property(String::class.java).convention(DEFAULT_SCHEMA_REGISTRY_URL)

        val searchAvroFilesPaths: SetProperty<String> =
            objects.setProperty(String::class.java)
                .convention(setOf(project.layout.buildDirectory.get().asFile.absolutePath))

        val topicToSchema: MapProperty<String, String> =
            objects.mapProperty(String::class.java, String::class.java)

        val subjectNameStrategy: Property<String> =
            objects.property(String::class.java).convention(DEFAULT_SUBJECT_NAME_STRATEGY)
    }

enum class SubjectNameStrategies {
    TopicNameStrategy,
    RecordNameStrategy,
    TopicRecordNameStrategy,
    ;

    companion object {
        fun from(name: String) = values().find { it.name == name }
    }
}
