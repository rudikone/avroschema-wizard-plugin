import io.github.rudikone.plugin.topic

plugins {
    java
    id("io.github.rudikone.avroschema-wizard-plugin")
//    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" to generate .avsc\.avpr from .avdl
}

avroWizardConfig {
    schemaRegistryUrl = "http://localhost:8081"
    configs {
        topic("my-first-topic") {
            searchAvroFilePath = "$projectDir/src/main/resources/avro"
            protocol = "ExampleProtocol"
            schema = "FirstExampleRecordFromProtocol"
            subjectNameStrategy = "TopicNameStrategy"
        }
        topic("my-second-topic") {
            searchAvroFilePath = "$projectDir/src/main/resources/avro"
            protocol = "ExampleProtocol"
            schema = "SecondExampleRecordFromProtocol"
            subjectNameStrategy = "RecordNameStrategy"
        }
        topic("my-third-topic") {
            searchAvroFilePath = "$projectDir/src/main/resources/avro"
            schema = "Example"
        }
    }
}
