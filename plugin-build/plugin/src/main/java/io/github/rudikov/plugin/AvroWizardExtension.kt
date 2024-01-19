package io.github.rudikov.plugin

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

const val DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:10081"

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

        val subjectToSchema: MapProperty<String, String> =
            objects.mapProperty(String::class.java, String::class.java)
    }
