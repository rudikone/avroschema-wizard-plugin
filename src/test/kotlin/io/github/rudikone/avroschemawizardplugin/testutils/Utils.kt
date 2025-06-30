package io.github.rudikone.avroschemawizardplugin.testutils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun randomString(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz"
    return (1..5).map { chars.random() }.joinToString("")
}

fun buildProject(
    projectDir: File,
    vararg arguments: String,
): BuildResult =
    GradleRunner.create()
        .withProjectDir(projectDir)
        .withEnvironment(System.getenv())
        .withArguments(listOf("--build-cache", "-i", "-s", *arguments))
        .forwardOutput()
        .build()
