@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.forward

import io.github.rudikone.avroschemawizardplugin.compatibility.BaseCompatibilityTest
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val COMPATIBILITY = "FORWARD"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Incompatible Avro schema changes are correctly rejected under FORWARD compatibility. Upgrade first: Producer")
class ForwardNotCompatibilityTest : BaseCompatibilityTest() {
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
                            { "name": "Name", "type": ["null", "string"] },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader type: INT not compatible with writer type: LONG",
            compatibility = COMPATIBILITY,
        )
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
                            { "name": "Age", "type": "int", "default": 0 },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "The field 'Name' at path '/fields/1' in the old schema has no default value and is missing in the new schema",
            compatibility = COMPATIBILITY,
        )
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
                            { "name": "Name", "type": ["null", "string"] },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "The old schema is missing enum symbols '[OTHER]' at path '/fields/2/type/symbols' in the new schema",
            compatibility = COMPATIBILITY,
        )
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
                            { "name": "Name", "type": ["null", "string", "int"] },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader union lacking writer type: INT",
            compatibility = COMPATIBILITY,
        )
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
                            { "name": "Age", "type": [ "int", "null"], "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader type: INT not compatible with writer type: NULL",
            compatibility = COMPATIBILITY,
        )
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

        checkNotCompatible(
            before = before,
            after = after,
            message = "reader type: INT not compatible with writer type: LONG",
            compatibility = COMPATIBILITY,
        )
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

        checkNotCompatible(
            before = before,
            after = after,
            message = "reader type: INT not compatible with writer type: LONG",
            compatibility = COMPATIBILITY,
        )
    }
}
