@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.full

import io.github.rudikone.avroschemawizardplugin.compatibility.BaseCompatibilityTest
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val COMPATIBILITY = "FULL"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Incompatible Avro schema changes are correctly rejected under FULL compatibility. Upgrade first: Any order")
class FullNotCompatibilityTest : BaseCompatibilityTest() {
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader type: INT not compatible with writer type: LONG",
            compatibility = COMPATIBILITY,
        )
    }

    @Test
    fun `enum value is removed`() {
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
                                    "type": "enum", "name": "Gender", "symbols": ["MALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "The new schema is missing enum symbols '[FEMALE]' at path '/fields/3/type/symbols' in the old schema",
            compatibility = COMPATIBILITY,
        )
    }

    @Test
    fun `optional field becomes required`() {
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader type: STRING not compatible with writer type: NULL",
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
            message = "The field 'LastName' at path '/fields/2' in the old schema has no default value and is missing in the new schema",
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
                            { "name": "Age", "type": ["int", "null"], "default": 0 },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader type: INT not compatible with writer type: NULL",
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "reader union lacking writer type: INT",
            compatibility = COMPATIBILITY,
        )
    }

    @Test
    fun `field without default value is added`() {
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
                            { "name": "MiddleName", "type": "string" }
                        ]
                    }
                    """.trimIndent(),
            )

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "The field 'MiddleName' at path '/fields/4' in the new schema has no default value and is missing in the old schema",
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
                            { "name": "LastName", "type": "string" },
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
            message = "The old schema is missing enum symbols '[OTHER]' at path '/fields/3/type/symbols' in the new schema",
            compatibility = COMPATIBILITY,
        )
    }

    @Test
    fun `field type long is narrowed to int`() {
        val before =
            record(
                """{ "name": "Age", "type": "long", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string", "int"] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )
        val after =
            record(
                """{ "name": "Age", "type": "int", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string", "int"] }""",
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
    fun `union branch is removed`() {
        val before =
            record(
                """{ "name": "Age", "type": "long", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string", "int"] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )
        val after =
            record(
                """{ "name": "Age", "type": "long", "default": 0 }""",
                """{ "name": "Name", "type": ["null", "string"] }""",
                """{ "name": "Gender", "type": { "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"] } }""",
            )

        checkNotCompatible(
            before = before,
            after = after,
            message = "reader union lacking writer type: INT",
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
    fun `field is renamed`() {
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
                            { "name": "FullName", "type": ["null", "string"] },
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

        checkNotCompatible(
            before = beforeChanges,
            after = afterChanges,
            message = "has no default value and is missing in the old schema",
            compatibility = COMPATIBILITY,
        )
    }
}
