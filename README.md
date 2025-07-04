# Avroschema-wizard-plugin (Gradle) 🐘

[![License](https://img.shields.io/github/license/cortinico/kotlin-android-template.svg)](LICENSE)
![Language](https://img.shields.io/github/languages/top/cortinico/kotlin-android-template?color=blue&logo=kotlin)
[![Download](https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)](https://plugins.gradle.org/plugin/https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)

Plugin to interact with schema-registry
using [API Schema registry](https://docs.confluent.io/platform/current/schema-registry/develop/api.html).

Simplify schema registration under specific subjects (topic names) and conduct seamless compatibility checks. This
plugin streamlines the integration of Avro schemas, enhancing your local testing process and ensuring smooth execution
in integration tests and CI pipelines. Use it now to effortlessly manage enrollment and schema validation in your Kafka
ecosystem!

## Use cases

The plugin simplifies kafka consumer testing when using avro schemas, and also provides additional features such as
compatibility check.
It can be used in local testing, as well as in integration tests or CI.

Features:

- registration of schemes under subjects
- compatibility check

## How to use 👣

build.gradle.kts:

```
plugins {
    id("io.github.rudikone.avroschema-wizard-plugin") version <version>
}

avroWizardConfig {
    schemaRegistryUrl.set("http://localhost:8081")
    configs {
        topic("my-first-topic") {
            searchAvroFilePath.set("$projectDir/src/main/resources/avro")
            protocol.set("ExampleProtocol")
            schema.set("FirstExampleRecordFromProtocol")
            subjectNameStrategy.set("TopicNameStrategy")
        }
        topic("my-second-topic") {
            searchAvroFilePath.set("$projectDir/src/main/resources/avro")
            protocol.set("ExampleProtocol")
            schema.set("SecondExampleRecordFromProtocol")
            subjectNameStrategy.set("RecordNameStrategy")
        }
        topic("my-third-topic") {
            searchAvroFilePath.set("$projectDir/src/main/resources/avro")
            schema.set("Example")
        }
    }
}
```

### Register schemas:

The `registerAllSchemas` task registers Avro schemas in the Schema Registry based on your plugin configuration.

```
./gradlew registerAllSchemas
```

**How It Works**:
- For each _**topic()**_ configuration:
    - If the **_protocol_** property is set, the task searches for a .avpr file with the name specified by **_protocol_** in the directory specified by **_searchAvroFilePath_**.
    - If only the **_schema_** property is set (and **_protocol_** is not), the task searches for a .avsc file with the name specified by **_schema_** in the **_searchAvroFilePath_** directory.
- The found schema is registered under the **_topic_** name (the name of the configuration) using the specified **_subjectNameStrategy_**.

This process is repeated for each _**topic**_ configuration.

**Output**:
For each successfully registered schema, the following message is printed:
`Registered <schema_name> with id: <id_from_registry> under subject <subject_name>`

**Notes**:
- If no matching file is found, the task will fail with an error.
- Each schema is registered independently for every configured topic.
- If a schema with the same name is registered under multiple subjects, the id will be assigned to it once. See [Documentation](https://docs.confluent.io/platform/current/schema-registry/develop/using.html#register-an-existing-schema-to-a-new-subject-name)

See [examples](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/RegisterTaskTest.kt) in tests.

### Compatibility check:

The `checkCompatibility` task verifies if your Avro schemas are compatible with the latest registered schema under a given subject in the Schema Registry.

**Batch Mode (for all topics)**

```
./gradlew checkCompatibility --compatibility=<compatibility>
```

**How It Works**:
- For each _**topic()**_ configuration:
    - If the **_protocol_** property is set, the task searches for a .avpr file with the name specified by **_protocol_** in the **_searchAvroFilePath_** directory.
    - If only the **_schema_** property is set (and **_protocol_** is not), the task searches for a .avsc file with the name specified by **_schema_** in the **_searchAvroFilePath_** directory.
- The found schema is checked for compatibility with the subject, which is derived from the **_topic_** name and **_subjectNameStrategy_**.
- The specified **_compatibility_** level is used for the check (if provided); otherwise, the default compatibility level is used.

This process is repeated for each **_topic_** configuration.


**Single Subject/Schema Mode**

```
./gradlew checkCompatibility --subject=<subject-name> --schema=<schema-name> --compatibility=<compatibility>
```

**How It Works**:
- The task searches for the **_topic_** configuration with the given **_schema_** name.
- If **_protocol_** is set in the configuration, it looks for a .avpr file; otherwise, it looks for a .avsc file as described above.
- The found schema is checked for compatibility with the specified **_subject_**.
- The specified **_compatibility_** level is used for the check (if provided); otherwise, the default compatibility level is used.
- If the **_subject_** does not exist in the Schema Registry, the task will fail with an error.

**Output**:
For each successfully registered schema, the following message is printed:
`Schema <schema_name> is (not) compatible with subject <subject_name>. Compatibility: <compatibility>`

**Notes**:
- If no matching file is found for the schema, the task will fail with an error.

See [examples](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/CompatibilityTaskTest.kt) in tests.

### Knowledge base :fire: :fire: :fire:

Use examples from the tests as a comprehensive knowledge base for [Schema Evolution and Compatibility](https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html):
- [BACKWARD](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/backward)
- [FORWARD](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/forward)
- [FULL](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/full)

### Properties:

avroWizardConfig:

| Name              | Description         | Default value            | Required |
|-------------------|---------------------|--------------------------|----------|
| schemaRegistryUrl | Schema registry URL | "http://localhost:10081" | -        |
| configs           | Configs for topics  | -                        | +        |

configs:

| Name                | Description                                                                           | Default value                  | Required |
|---------------------|---------------------------------------------------------------------------------------|--------------------------------|----------|
| searchAvroFilePath  | Directory to search for file with extension .avcs or .avpr                            | build directory of the project | -        |
| protocol            | Name of .avpr file                                                                    | -                              | -        |
| schema              | Name of .avsc file or record in protocol                                              | -                              | +        |
| subjectNameStrategy | Subject Name Strategy: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy | TopicNameStrategy              | -        |

## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.

## License 📄

This plugin is licensed under the MIT License - see the [License](LICENSE) file for details.
