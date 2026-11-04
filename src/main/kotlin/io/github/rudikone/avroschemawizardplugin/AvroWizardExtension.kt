package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.SubjectNameStrategies.TopicNameStrategy
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

const val DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:10081"
val DEFAULT_SUBJECT_NAME_STRATEGY = TopicNameStrategy.name

abstract class AvroWizardExtension
    @Inject
    constructor(
        objects: ObjectFactory,
        layout: ProjectLayout,
    ) {
        val schemaRegistryUrl: Property<String> =
            objects.property(String::class.java).convention(DEFAULT_SCHEMA_REGISTRY_URL)

        val defaultSearchPath: Provider<String> =
            layout.buildDirectory.map { it.asFile.absolutePath }

        val subjectConfigs: NamedDomainObjectContainer<SubjectConfig> =
            objects.domainObjectContainer(SubjectConfig::class.java)

        fun configs(action: Action<NamedDomainObjectContainer<SubjectConfig>>) {
            action.execute(subjectConfigs)
        }
    }

abstract class SchemaReferenceConfig {
    abstract val name: Property<String>

    abstract val subject: Property<String>

    abstract val version: Property<Int>
}

abstract class SubjectConfig
    @Inject
    constructor(
        private val name: String,
        private val objects: ObjectFactory,
    ) : Named {
        abstract val searchAvroFilePath: Property<String>

        abstract val protocol: Property<String>

        abstract val schema: Property<String>

        abstract val subjectNameStrategy: Property<String>

        abstract val references: ListProperty<SchemaReferenceConfig>

        init {
            subjectNameStrategy.convention(DEFAULT_SUBJECT_NAME_STRATEGY)
        }

        fun references(action: Action<ListProperty<SchemaReferenceConfig>>) {
            action.execute(references)
        }

        fun ListProperty<SchemaReferenceConfig>.reference(config: SchemaReferenceConfig.() -> Unit) {
            add(objects.newInstance(SchemaReferenceConfig::class.java).apply(config))
        }

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
