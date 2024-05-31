import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.shadow)
    java
}

gradlePlugin {
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            version = property("VERSION").toString()
            description = property("DESCRIPTION").toString()
            displayName = property("DISPLAY_NAME").toString()
            tags.set(listOf("avro", "schema-registry"))
        }
    }

    website.set(property("WEBSITE").toString())
    vcsUrl.set(property("VCS_URL").toString())
}

dependencies {
    implementation(kotlin("stdlib"))
    shadow(gradleApi())
    implementation(libs.schemaRegistryClient)
    implementation(libs.kafkaSchemaSerializer)

    testImplementation(libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    create("setupPluginUploadFromEnvironment") {
        doLast {
            val key = System.getenv("GRADLE_PUBLISH_KEY")
            val secret = System.getenv("GRADLE_PUBLISH_SECRET")

            if (key == null || secret == null) {
                throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
            }

            System.setProperty("gradle.publish.key", key)
            System.setProperty("gradle.publish.secret", secret)
        }
    }

    named("shadowJar", ShadowJar::class) {
        archiveClassifier.set("")
        isEnableRelocation = true
        relocationPrefix = "avrowizard"
        manifest {
            attributes["Main-Class"] = "io.github.rudikone.plugin.AvroSchemaWizardPluginKt"
        }
    }
}
