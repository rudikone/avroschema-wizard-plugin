import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.shadow)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versionCheck)
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

group = property("GROUP").toString()
version = property("VERSION").toString()

ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    shadow(gradleApi())
    implementation(libs.schemaRegistryClient)
    implementation(libs.kafkaSchemaSerializer)

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainersKafka)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    register("reformatAll") {
        description = "Reformat all the Kotlin Code"
        dependsOn("ktlintFormat")
    }

    withType<Detekt> {
        reports {
            html.required.set(true)
            html.outputLocation.set(file("build/reports/detekt.html"))
        }
    }

    /*
     * It is needed to ensure that tests always verify the current version of the plugin.
     * See io.github.rudikone.avroschemawizardplugin.RegisterTaskTest and io.github.rudikone.avroschemawizardplugin.testutils.Plugin
     */
    test {
        dependsOn(publishToMavenLocal)
        useJUnitPlatform {
            environment.putIfAbsent("avroschema-wizard-plugin-version", project.version)
        }
    }

    named("shadowJar", ShadowJar::class) {
        archiveClassifier.set("")
        isEnableRelocation = false
        relocate("io.confluent", "avrowizard.io.confluent")
        relocate("org.apache.avro", "avrowizard.org.apache.avro")
        manifest {
            attributes["Main-Class"] = "io.github.rudikone.avroschemawizardplugin.AvroSchemaWizardPlugin"
        }
    }

    register("preMerge") {
        description = "Runs all the tests/verification tasks"
        dependsOn("check")
        dependsOn("validatePlugins")
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

    withType<DependencyUpdatesTask> {
        rejectVersionIf {
            candidate.version.isNonStable()
        }
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

fun String.isNonStable() = "^[0-9,.v-]+(-r)?$".toRegex().matches(this).not()
