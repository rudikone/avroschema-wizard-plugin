plugins {
    java
    id("ru.rudikov.avroschema-wizard-plugin")
}

avroWizardConfig {
    schemaRegistryUrl = "http://localhost:8081"
    searchAvroFilesPaths = setOf("$projectDir/src/main/resources")
    subjectToSchema = mapOf("my-topic" to "Example")
}
