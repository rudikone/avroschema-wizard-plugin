package io.github.rudikone.avroschemawizardplugin.testutils

data class Avro(
    val name: String,
    val payLoad: String,
)

fun exampleProtocol() =
    Avro(
        name = "ExampleProtocol.avpr",
        payLoad =
            """
            {
                "protocol" : "ExampleProtocol",
                "namespace" : "ru.rudikov.example",
                "types" : [ {
                    "type" : "record",
                    "name" : "FirstExampleRecordFromProtocol",
                    "fields" : [ {
                        "name" : "name",
                        "type" : "string"
                    }, {
                        "name" : "age",
                        "type" : "int"
                    }, {
                        "name" : "userName",
                        "type" : [ "null", "string" ],
                        "default" : null
                    } ]
                }, {
                    "type" : "record",
                    "name" : "SecondExampleRecordFromProtocol",
                    "fields" : [ {
                        "name" : "name",
                        "type" : "string"
                    }, {
                        "name" : "color",
                        "type" : "string"
                    } ]
                } ],
                "messages" : { }
            }
            """.trimIndent(),
    )

fun exampleSchema() =
    Avro(
        name = "Example.avsc",
        payLoad =
            """
            {
                "type": "record",
                "namespace": "ru.rudikov.example",
                "name": "ExampleRecordFromSchema",
                "fields": [
                    { "name": "Name", "type": "string" },
                    { "name": "Age", "type": "int" },
                    { "name": "userName", "type": [ "null", "string" ], "default": null }
                ]
            }
            """.trimIndent(),
    )
