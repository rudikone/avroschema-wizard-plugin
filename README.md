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

## License 📄

This plugin is licensed under the MIT License - see the [License](LICENSE) file for details.

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
```

### Register schemas:

run

```
gradle registerAllSchemas
```

The task searches for a .avrp file with the name **_protocol_** on the path **_searchAvroFilePath_** and registers *
*_schema_** under the _**topic config name**_ by the **_subjectNameStrategy_** strategy.

If you specify only **_schema_** and do not specify _**protocol**_, then the task searches for a .avsc file with the
name **_schema_** on the path **_searchAvroFilePath_** and registers **_schema_** under the _**topic config name**_ by
the **_subjectNameStrategy_** strategy.

This action is performed for each `topic()` configuration

***Output***: Registered <schema_name> with id: <id_from_registry> for <topic_name>

***If a schema with the same name is registered under multiple subjects, the id will be assigned to it once.
See [Documentation](https://docs.confluent.io/platform/current/schema-registry/develop/using.html#register-an-existing-schema-to-a-new-subject-name)
***

### Compatibility check:

run

```
gradle checkCompatibility
```

The task searches for a .avrp file with the name **_protocol_** on the path **_searchAvroFilePath_** and checks if the
current **_schema_** is compatible with the subject under **_topic config name_** on the **_subjectNameStrategy_**
strategy.

If you specify only **_schema_** and do not specify _**protocol**_, then the task searches for a .avsc file with the
name **_schema_** on the path **_searchAvroFilePath_** and checks if the current **_schema_** is compatible with the
subject under **_topic config name_** on the **_subjectNameStrategy_** strategy.

This action is performed for each `topic()` configuration

OR

run

```
gradle checkCompatibility --subject=<subject-name> --schema=<schema-name>
```

The task searches for **_topic name config_** with **_schema_**, then searches for a .avpr file with the name *
*_protocol_**
on the path **_searchAvroFilePath_** and checks if the current _**schema**_ is compatible with the **_subject_**.

If you specify only **_schema_** and do not specify _**protocol**_, then the task searches for a .avsc file with the
name **_schema_** on the path **_searchAvroFilePath_** checks if the current _**schema**_ is compatible with the *
*_subject_**.

***If the subject does not exist, an error will be thrown!***

***Output***: Schema <schema_name> is (not) compatible with the latest schema under subject <subject_name>

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

### Example:

See [example](example/build.gradle.kts) module. Run [docker-compose](example/docker-compose.yaml), testing plugin tasks.
For convenience, you can use schema-registry-ui (http://localhost:8002/ in your browser).

## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.
