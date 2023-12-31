plugins {
    java
    id("ru.rudikov.avroschema-wizard-plugin")
}

avroWizardConfig {
    subjectToSchema = mapOf("my-topic" to "Schema")
}
