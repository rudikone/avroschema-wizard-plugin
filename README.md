# Avroschema-wizard-plugin (Gradle) 🐘

[![License](https://img.shields.io/github/license/cortinico/kotlin-android-template.svg)](LICENSE)
![Language](https://img.shields.io/github/languages/top/cortinico/kotlin-android-template?color=blue&logo=kotlin)
[![Download](https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)](https://plugins.gradle.org/plugin/https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)

Plugin to interact with schema-registry using [API Schema registry](https://docs.confluent.io/platform/current/schema-registry/develop/api.html).

Simplify schema registration under specific subjects (topic names) and conduct seamless compatibility checks. This plugin streamlines the integration of Avro schemas, enhancing your local testing process and ensuring smooth execution in integration tests and CI pipelines. Use it now to effortlessly manage enrollment and schema validation in your Kafka ecosystem!

## License 📄

This plugin is licensed under the MIT License - see the [License](LICENSE) file for details.

## Use cases

The plugin simplifies kafka consumer testing when using avro schemas, and also provides additional features such as compatibility check.
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
    schemaRegistryUrl = "some_url"
    searchAvroFilesPaths = setOf("../some/search/path/1", "../some/search/path/2")
    topicToSchema = mapOf("some-topic-1" to "SomeSchemaName1", "some-topic-2" to "SomeSchemaName2")
    subjectNameStrategy = "TopicNameStrategy"
}
```
### Register schemas:

run
```
gradle registerAllSchemas
```

The task will search for files with extension .avsc, whose names (without extension) are passed as __topicToSchema__
map values, in the directories passed to __searchAvroFilesPaths__, and register schemas by appropriate subjects
depending on the __subjectNameStrategy__ value.

***Output***: Registered <schema_name> with id: <id_from_registry> for <topic_name>

***If a schema with the same name is registered under multiple subjects, the id will be assigned to it once.
See [Documentation](https://docs.confluent.io/platform/current/schema-registry/develop/using.html#register-an-existing-schema-to-a-new-subject-name)***


### Compatibility check:

run
```
gradle checkCompatibility
```

The task will search for files with extension .avsc, whose names (without extension) are passed as __topicToSchema__
map values, in the directories passed to __searchAvroFilesPaths__, and check the compatibility of the new versions of
the schemas by appropriate subjects depending on the __subjectNameStrategy__ value.

OR

run
```
gradle checkCompatibility --subject=<subject-name> --schema=<schema-name>
```

The task will search for a file with the extension .avsc, whose name (without extension) is passed as a command line
argument (schema), in the directories given in __searchAvroFilesPaths__, and check if the passed schema is compatible
with the schema under the subject passed as a command line argument (subject).

***If the subject does not exist, an error will be thrown!***

***Output***: Schema <schema_name> is (not) compatible with the latest schema under subject <subject_name>

### Properties:

| Name                 | Description                                                                           | Default value                  |
|----------------------|---------------------------------------------------------------------------------------|--------------------------------|
| schemaRegistryUrl    | Schema registry URL                                                                   | "http://localhost:10081"       |
| searchAvroFilesPaths | List of directories to search for files with extension .avcs                          | build directory of the project |
| subjectToSchema      | Subject (topic) to schema name map                                                    | -                              |
| subjectNameStrategy  | Subject Name Strategy: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy | TopicNameStrategy              |

### Example:

See [example](example/build.gradle.kts) module. Run [docker-compose](example/docker-compose.yaml), testing plugin tasks.
For convenience, you can use schema-registry-ui (http://localhost:8002/ in your browser).

## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.
