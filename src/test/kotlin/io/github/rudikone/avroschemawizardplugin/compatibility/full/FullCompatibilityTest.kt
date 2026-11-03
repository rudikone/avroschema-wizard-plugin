@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.full

import io.github.rudikone.avroschemawizardplugin.compatibility.BaseCompatibilityTest
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val COMPATIBILITY = "FULL"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("All allowed Avro schema evolutions are compatible under FULL compatibility. Upgrade first: Any order")
class FullCompatibilityTest : BaseCompatibilityTest() {
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
                        { "name": "Name", "type": ["null", "string"] },
                        { "name": "LastName", "type": "string" },
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
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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
}
