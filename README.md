# Avroschema-wizard-plugin (Gradle) 🐘

[![License](https://img.shields.io/github/license/cortinico/kotlin-android-template.svg)](LICENSE)
![Language](https://img.shields.io/github/languages/top/cortinico/kotlin-android-template?color=blue&logo=kotlin)
[![Download](https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)](https://plugins.gradle.org/plugin/https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin)

Plugin to interact with schema-registry using [API Schema registry](https://docs.confluent.io/platform/current/schema-registry/develop/api.html).

## License 📄

This plugin is licensed under the MIT License - see the [License](LICENSE) file for details.

## Use cases

The plugin simplifies kafka consumer testing when using avro schemas, and also provides additional features such as compatibility check.
It can be used in local testing, as well as in integration tests or CI.

Features:
- registration of schemes under subjects (topic-name)
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
    subjectToSchema = mapOf("some-topic-1" to "SomeSchemaName1", "some-topic-2" to "SomeSchemaName2")
}
```
### Register schemas:

run
```
gradle registerAllSchemas
```

The task will search for files with extension .avsc, whose names (without extension) are passed as __subjectToSchema__
map values, in the directories passed to __searchAvroFilesPaths__, and register schemas by the corresponding subjects
passed as __subjectToSchema__ map keys.

***Output***: <schema_name>: <id_from_registry>

***If a schema with the same name is registered under multiple subjects, the id will be assigned to it once.
See [Documentation](https://docs.confluent.io/platform/current/schema-registry/develop/using.html#register-an-existing-schema-to-a-new-subject-name)***


### Compatibility check:

run
```
gradle checkCompatibility
```

The task will search for files with extension .avsc, whose names (without extension) are passed as __subjectToSchema__
map values, in the directories passed to __searchAvroFilesPaths__, and check the compatibility of the new versions of
the schemas by the corresponding subjects passed as __subjectToSchema__ map keys.

OR

run
```
gradle checkCompatibility --topic=<topic-name> --schema=<schema-name>
```

The task will search for a file with the extension .avsc, whose name (without extension) is passed as a command line
argument (schema), in the directories given in __searchAvroFilesPaths__, and check if the passed schema is compatible
with the schema under the subject passed as a command line argument (topic).

***If the subject does not exist, an error will be thrown!***

***Output***: Schema <schema_name> is (not) compatible with the latest schema under subject <subject_name>

### Properties:

| Name                 | Description                                                  | Default value                  |
|----------------------|--------------------------------------------------------------|--------------------------------|
| schemaRegistryUrl    | Schema registry URL                                          | "http://localhost:10081"       |
| searchAvroFilesPaths | List of directories to search for files with extension .avcs | build directory of the project |
| subjectToSchema      | Subject (topic) to schema name map                           | -                              |

### Example:

See [example](example/build.gradle.kts) module. Run [docker-compose](example/docker-compose.yaml), testing plugin tasks.
For convenience, you can use schema-registry-ui (http://localhost:8002/ in your browser).


## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.
