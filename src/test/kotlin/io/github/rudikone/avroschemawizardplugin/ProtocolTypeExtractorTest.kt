package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.testutils.referencingProtocol
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("MaxLineLength")
class ProtocolTypeExtractorTest {
    @Test
    fun `extracts target type json and injects protocol namespace`() {
        val avpr = referencingProtocol().payLoad

        val json = extractTypeFromProtocol(avpr, "MainRecordFromProtocol", "ReferencingProtocol")

        assertTrue(json.contains("\"name\":\"MainRecordFromProtocol\""), "Expected the record name.\nActual: $json")
        assertTrue(json.contains("\"namespace\":\"ru.rudikov.reference\""), "Expected injected namespace.\nActual: $json")
        assertTrue(json.contains("ru.rudikov.reference.SharedRecord"), "Expected the external field type.\nActual: $json")
    }

    @Test
    fun `throws clear error when type is not found`() {
        val avpr = """{ "protocol": "P", "types": [ { "type": "record", "name": "Other", "fields": [] } ] }"""

        val e =
            assertThrows(IllegalStateException::class.java) {
                extractTypeFromProtocol(avpr, "Missing", "P")
            }
        assertTrue("Type Missing not found in protocol P" in e.message!!, "Unexpected message: ${e.message}")
    }
}
