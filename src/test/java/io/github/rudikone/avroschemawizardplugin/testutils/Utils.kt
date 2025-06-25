package io.github.rudikone.avroschemawizardplugin.testutils

fun randomString(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz"
    return (1..5).map { chars.random() }.joinToString("")
}
