@file:Suppress("MaxLineLength")

package io.github.rudikone.avroschemawizardplugin.testutils

open class Dependency(private val dependencyName: String) {
    constructor(group: String, name: String, version: String?) :
        this("\"$group:$name${version?.let { ":$version" } ?: ""}\"")

    override fun toString(): String = dependencyName
}

class Implementation(dependencyName: String) :
    Dependency("implementation($dependencyName)") {
    constructor(group: String, name: String, version: String?) :
        this("\"$group:$name${version?.let { ":$version" } ?: ""}\"")
}

class TestImplementation(dependencyName: String) :
    Dependency("testImplementation($dependencyName)") {
    constructor(group: String, name: String, version: String?) :
        this("\"$group:$name${version?.let { ":$version" } ?: ""}\"")
}

fun kotlinStdlibJdk8(version: String? = null) = Implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", version)

fun springBootStarterWeb(version: String? = null) = Implementation("org.springframework.boot", "spring-boot-starter-web-services", version)

fun springBootStarterActuator(version: String? = null) = Implementation("org.springframework.boot", "spring-boot-starter-actuator", version)

fun junit(version: String? = null): MutableList<Dependency> =
    mutableListOf(
        TestImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = version),
        TestImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = version),
        TestImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = version),
    )

fun defaultSpringDependency(): MutableList<Dependency> =
    mutableListOf<Dependency>(
        kotlinStdlibJdk8(),
        springBootStarterWeb(),
        springBootStarterActuator(),
    ).also {
        it.addAll(junit())
    }
