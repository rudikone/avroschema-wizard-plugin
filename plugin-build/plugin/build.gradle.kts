import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
}

gradlePlugin {
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            version = property("VERSION").toString()
            description = property("DESCRIPTION").toString()
            displayName = property("DISPLAY_NAME").toString()
            tags.set(listOf("plugin", "gradle"))
        }
    }

    website.set(property("WEBSITE").toString())
    vcsUrl.set(property("VCS_URL").toString())
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(libs.schemaRegistryClient)

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
}
