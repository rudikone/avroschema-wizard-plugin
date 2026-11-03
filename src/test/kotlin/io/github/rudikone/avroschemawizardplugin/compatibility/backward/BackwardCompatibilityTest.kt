@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.backward

import io.github.rudikone.avroschemawizardplugin.compatibility.BaseCompatibilityTest
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val COMPATIBILITY = "BACKWARD"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("All allowed Avro schema evolutions are compatible under BACKWARD compatibility. Upgrade first: Consumer")
class BackwardCompatibilityTest : BaseCompatibilityTest() {
    private val beforeChanges =
        Avro(
            name = "Example.avsc",
            payLoad =
                """
                {
                  "type": "record",
                  "namespace": "ru.rudikov.example",
                  "name": "Example",
                  "fields": [
                    { "name": "Age", "type": "int", "default": 0 },
                    { "name": "Name", "type": "string" },
                    { "name": "LastName", "type": ["null", "string"] },
                    {
                        "name": "Gender",
                        "type": {
                            "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                         }
                    }
                  ]
                }
                """.trimIndent(),
        )

    @Test
    fun `field type int extended to long`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                      "type": "record",
                      "namespace": "ru.rudikov.example",
                      "name": "Example",
                      "fields": [
                        { "name": "Age", "type": "long", "default": 0 },
                        { "name": "Name", "type": "string" },
                        { "name": "LastName", "type": ["null", "string"] },
                        {
                            "name": "Gender",
                            "type": {
                                "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                             }
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `record field with nested defaults is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age","type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            {
                                "name": "Address",
                                "type": {
                                    "type": "record",
                                    "name": "AddressRecord",
                                    "fields": [
                                        { "name": "City", "type": "string", "default": "Unknown" }
                                    ]
                                },
                                "default": {}
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `enum field with default is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            {
                                "name": "Status",
                                "type": {
                                    "type": "enum",
                                    "name": "StatusEnum",
                                    "symbols": ["ACTIVE", "INACTIVE"]
                                },
                                "default": "INACTIVE"
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `enum value is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                      "type": "record",
                      "namespace": "ru.rudikov.example",
                      "name": "Example",
                      "fields": [
                        { "name": "Age", "type": "int", "default": 0 },
                        { "name": "Name", "type": "string" },
                        { "name": "LastName", "type": ["null", "string"] },
                        {
                            "name": "Gender",
                            "type": {
                                "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE", "OTHER"]
                             }
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `array field with default is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age","type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            {
                                "name": "Hobbies",
                                "type": {
                                    "type": "array",
                                    "items": "string"
                                },
                                "default": []
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `union field with default is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "Score", "type": ["null", "int"], "default": null }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `field with default value is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "MiddleName", "type": "string", "default": "" }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `field with default is removed`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Name", "type": "string" },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `field without default is removed`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int","default": 0 },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `required field becomes optional`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `new union type is added`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                      "type": "record",
                      "namespace": "ru.rudikov.example",
                      "name": "Example",
                      "fields": [
                        { "name": "Age", "type": "int", "default": 0 },
                        { "name": "Name", "type": "string" },
                        { "name": "LastName", "type": ["null", "string", "int"] },
                        {
                            "name": "Gender",
                            "type": {
                                "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                             }
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        checkCompatible(beforeChanges, afterChanges, COMPATIBILITY)
    }

    @Test
    fun `array field item type int extended to long`() {
        val before =
            record(
                """{ "name": "Age", "type": "int", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string"] }""",
                """{ "name": "Tags", "type": { "type": "array", "items": "int" }, "default": [] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )
        val after =
            record(
                """{ "name": "Age", "type": "int", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string"] }""",
                """{ "name": "Tags", "type": { "type": "array", "items": "long" }, "default": [] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )

        checkCompatible(before, after, COMPATIBILITY)
    }

    @Test
    fun `map field value type int extended to long`() {
        val before =
            record(
                """{ "name": "Age", "type": "int", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string"] }""",
                """{ "name": "Labels", "type": { "type": "map", "values": "int" }, "default": {} }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )
        val after =
            record(
                """{ "name": "Age", "type": "int", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string"] }""",
                """{ "name": "Labels", "type": { "type": "map", "values": "long" }, "default": {} }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )

        checkCompatible(before, after, COMPATIBILITY)
    }

    @Test
    fun `field default value is changed`() {
        val after =
            record(
                """{ "name": "Age", "type": "int", "default": 1 }""",
                """{ "name": "Name", "type": "string" }""",
                """{ "name": "LastName", "type": ["null", "string"] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )

        checkCompatible(beforeChanges, after, COMPATIBILITY)
    }
}
