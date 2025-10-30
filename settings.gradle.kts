pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven")
        }
    }
}

plugins {
    id("com.gradle.develocity") version "3.19.2"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"

        publishing.onlyIf {
            System.getenv("GITHUB_ACTIONS") == "true"
        }

        publishing.onlyIf {
            it.buildResult.failures.isNotEmpty()
        }
    }
}

rootProject.name = "avroschema-wizard-plugin"
