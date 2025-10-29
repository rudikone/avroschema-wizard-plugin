package io.github.rudikone.avroschemawizardplugin.testutils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

private const val DEFAULT_GRADLE_VERSION = "8.14.3"

fun randomString(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz"
    return (1..5).map { chars.random() }.joinToString("")
}

fun buildProject(
    projectDir: File,
    gradleVersion: String = DEFAULT_GRADLE_VERSION,
    vararg arguments: String,
): BuildResult =
    GradleRunner
        .create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(projectDir)
        .withEnvironment(System.getenv())
        .withArguments(listOf("--build-cache", "-i", "-s", *arguments))
        .forwardOutput()
        .build()
