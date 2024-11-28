package io.github.rudikone.plugin

import io.github.rudikone.plugin.SubjectNameStrategies.TopicNameStrategy
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

const val DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:10081"
val DEFAULT_SUBJECT_NAME_STRATEGY = TopicNameStrategy.name

abstract class AvroWizardExtension(
    @Inject private val project: Project,
) {
    private val objects = project.objects

    val schemaRegistryUrl: Property<String> =
        objects.property(String::class.java).convention(DEFAULT_SCHEMA_REGISTRY_URL)

    val subjectConfigs: NamedDomainObjectContainer<SubjectConfig> =
        objects.domainObjectContainer(SubjectConfig::class.java)

    fun configs(action: Action<NamedDomainObjectContainer<SubjectConfig>>) {
        action.execute(subjectConfigs)
    }
}

abstract class SubjectConfig(
    @Inject private val project: Project,
    private val name: String,
) : Named {
    private val objects = project.objects

    @get:Input
    @get:Optional
    val searchAvroFilePath: Property<String> =
        objects.property(String::class.java).convention(project.layout.buildDirectory.get().asFile.absolutePath)

    @get:Input
    val protocol: Property<String> = objects.property(String::class.java)

    @get:Input
    val schema: Property<String> = objects.property(String::class.java)

    @get:Input
    @get:Optional
    val subjectNameStrategy: Property<String> =
        objects.property(String::class.java).convention(DEFAULT_SUBJECT_NAME_STRATEGY)

    override fun getName(): String = name
}

fun NamedDomainObjectContainer<SubjectConfig>.topic(
    name: String,
    config: SubjectConfig.() -> Unit,
) {
    create(name, config)
}

enum class SubjectNameStrategies {
    TopicNameStrategy,
    RecordNameStrategy,
    TopicRecordNameStrategy,
    ;

    companion object {
        fun from(name: String) = entries.find { it.name == name }
    }
}
