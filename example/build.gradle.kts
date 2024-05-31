plugins {
    java
    id("io.github.rudikone.avroschema-wizard-plugin")
}

avroWizardConfig {
    schemaRegistryUrl = "http://localhost:8081"
    searchAvroFilesPaths = setOf("$projectDir/src/main/resources/avro", "$projectDir/src/main/resources/empty")
    topicToSchema = mapOf("my-topic" to "Example")
}
