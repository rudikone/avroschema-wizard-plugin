<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/logo.png">
    <img src="assets/logo.png" width="220" alt="Wizard">
  </picture>
</p>

<h1 align="center">Avroschema-wizard-plugin</h1>

<p align="center">
  <a href="https://github.com/rudikone/avroschema-wizard-plugin/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/rudikone/avroschema-wizard-plugin" alt="License">
  </a>
  <img src="https://img.shields.io/github/languages/top/rudikone/avroschema-wizard-plugin?color=blue&logo=kotlin" alt="Language">
  <a href="https://plugins.gradle.org/plugin/io.github.rudikone.avroschema-wizard-plugin">
    <img src="https://img.shields.io/gradle-plugin-portal/v/io.github.rudikone.avroschema-wizard-plugin" alt="Download">
  </a>
  <a href="https://deepwiki.com/rudikone/avroschema-wizard-plugin">
    <img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki">
  </a>
</p>

Gradle plugin for registering Avro schemas to Schema Registry and checking their compatibility.

**Full documentation:** [DeepWiki](https://deepwiki.com/rudikone/avroschema-wizard-plugin/)

---

## ⚡ Quick Start

```kotlin
// build.gradle.kts
plugins {
    id("io.github.rudikone.avroschema-wizard-plugin") version "3.3.0"
}

avroWizardConfig {
    schemaRegistryUrl.set("http://localhost:8081")
    configs {
        topic("my-topic") {
            schema.set("MyRecord")
        }
    }
}
```

```bash
# Register schemas
./gradlew registerAllSchemas

# Check compatibility
./gradlew checkCompatibility --compatibility=BACKWARD
```

---

## Version Matrix

| Component                              | Version |
|----------------------------------------|---------|
| Gradle (min)                           | 8.4     |
| Java (min)                             | 17      |
| confluent:kafka-schema-registry-client | 8.1.1   |
| confluent:kafka-schema-serializer      | 8.1.1   |

**Configuration Cache Support:** since version 3.3.0

---

## Features

- Register Avro schemas to Schema Registry
- Schema compatibility checking (BACKWARD, FORWARD, FULL)
- Support for .avsc files and .avpr protocols
- Three subject naming strategies: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy
- Configuration Cache compatible
- Local testing and CI/CD integration

---

## 🚀 How to Use

### 1. Plugin Setup

**build.gradle.kts:**

```kotlin
plugins {
    id("io.github.rudikone.avroschema-wizard-plugin") version "3.3.0"
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
            // subjectNameStrategy defaults to TopicNameStrategy
        }
    }
}
```

**build.gradle (Groovy):**

```groovy
plugins {
    id 'io.github.rudikone.avroschema-wizard-plugin' version '3.3.0'
}

avroWizardConfig {
    schemaRegistryUrl.set('http://localhost:8081')
    configs {
        topic('my-topic') {
            schema.set('MyRecord')
        }
    }
}
```

### 2. Configuration

#### avroWizardConfig

| Property            | Description          | Default Value            | Required |
|---------------------|----------------------|--------------------------|----------|
| `schemaRegistryUrl` | Schema Registry URL  | `http://localhost:10081` | ❌        |
| `configs`           | Topic configurations | —                        | ✅        |

#### Topic Configuration (configs)

| Property              | Description                                                                    | Default Value           | Required |
|-----------------------|--------------------------------------------------------------------------------|-------------------------|----------|
| `searchAvroFilePath`  | Directory to search for .avsc/.avpr files                                      | `<buildDir>` of project | ❌        |
| `protocol`            | Name of .avpr file (without extension)                                         | —                       | ❌¹       |
| `schema`              | Schema name (.avsc) or record from protocol                                    | —                       | ✅        |
| `subjectNameStrategy` | Strategy: `TopicNameStrategy`, `RecordNameStrategy`, `TopicRecordNameStrategy` | `TopicNameStrategy`     | ❌        |

¹ — `protocol` and `schema` (without protocol) are interchangeable: specify either protocol+schema, or just schema.

---

## Tasks

### registerAllSchemas

Registers Avro schemas to Schema Registry based on configuration.

```bash
./gradlew registerAllSchemas
```

**How it works:**

1. For each `topic()` configuration:
    - If `protocol` is specified — searches for `.avpr` file in `searchAvroFilePath`
    - If only `schema` is specified — searches for `.avsc` file in `searchAvroFilePath`
2. Registers the schema under a subject name formed by `subjectNameStrategy`

**Output:**

```
Registered FirstExampleRecordFromProtocol with id: 123 under subject my-topic-value
```

**Notes:**

- If file is not found — task fails with error
- Each schema is registered independently for each topic
- When registering the same schema under different subjects, ID is assigned once

📖 [Test Examples](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/RegisterTaskTest.kt)

---

### checkCompatibility

Verifies Avro schema compatibility with the latest version in Schema Registry.

#### Batch Mode (all topics)

```bash
./gradlew checkCompatibility --compatibility=BACKWARD
```

Available compatibility levels: `BACKWARD`, `FORWARD`, `FULL`, `NONE`

**How it works:**

1. For each `topic()` configuration:
    - Finds schema (by protocol or schema)
    - Checks compatibility against current version in Schema Registry
    - Uses specified compatibility level (or default from SR)

**Output:**

```
Schema ru.rudikov.example.Example is compatible with subject my-topic-value. Compatibility: BACKWARD
```

When incompatible:

```
Schema ru.rudikov.example.Example is not compatible with subject my-topic-value. Compatibility: BACKWARD
 → {errorType:'TYPE_MISMATCH', description:'The type (path '/fields/0/type') of a field in the new schema does not match with the old schema', additionalInfo:'reader type: STRING not compatible with writer type: INT'}
```

#### Single Schema Mode

```bash
./gradlew checkCompatibility --subject=my-topic-value --schema=MyRecord --compatibility=BACKWARD
```

**Notes:**

- If subject does not exist in Schema Registry — task fails with error
- If schema file is not found — task fails with error

📖 [Test Examples](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/CompatibilityTaskTest.kt)

---

## 🧪 Testing

### Local Testing

Use a test Schema Registry instance:

```bash
# Run via Docker
docker run -d --name schema-registry -p 8081:8081 confluentinc/cp-schema-registry:latest
```

### Integration Tests

The plugin is tested using built-in test framework. See examples:

- [Schema Registration](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/RegisterTaskTest.kt)
- [Compatibility Check](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/CompatibilityTaskTest.kt)

### Compatibility Knowledge Base

Detailed schema evolution and compatibility check examples:

- [BACKWARD](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/backward/)
- [FORWARD](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/forward/)
- [FULL](src/test/kotlin/io/github/rudikone/avroschemawizardplugin/compatibility/full/)

📖 [Confluent Docs: Schema Evolution](https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html)

---

## 🔧 Troubleshooting

### Error: "File not found in configured search path(s)!"

**Cause:** .avsc or .avpr file not found in specified directory.

**Solution:**

```kotlin
topic("my-topic") {
    // Specify correct path to files
    searchAvroFilePath.set("$projectDir/src/main/resources/avro")
    schema.set("MyRecord") // Ensure MyRecord.avsc exists
}
```

### Error: "Unsupported subject name strategy"

**Cause:** Invalid subject naming strategy specified.

**Solution:** Use one of the allowed values:

- `TopicNameStrategy`
- `RecordNameStrategy`
- `TopicRecordNameStrategy`

### Error: "Topic configs is empty!"

**Cause:** No topics configured in `configs`.

**Solution:**

```kotlin
avroWizardConfig {
    configs {
        topic("my-topic") {
            schema.set("MyRecord")
        }
    }
}
```

### Schema Registry Connection Error

**Cause:** Incorrect URL or service unavailable.

**Solution:**

- Verify `schemaRegistryUrl` in configuration
- Ensure Schema Registry is running and accessible
- Check network settings and firewall

### Configuration Cache: Caching Issues

**Cause:** Non-cacheable elements in configuration.

**Solution:** Since version 3.3.0, the plugin fully supports Configuration Cache. If issues occur:

```bash
./gradlew build --configuration-cache --refresh-configuration-cache
```

---

## CI/CD Integration

### GitHub Actions

```yaml
-   name: Register Avro Schemas
    run: ./gradlew registerAllSchemas
    env:
        SCHEMA_REGISTRY_URL: ${{ secrets.SCHEMA_REGISTRY_URL }}

-   name: Check Compatibility
    run: ./gradlew checkCompatibility --compatibility=BACKWARD
```

### GitLab CI

```yaml
avro-check:
    script:
        - ./gradlew checkCompatibility --compatibility=BACKWARD
    variables:
        SCHEMA_REGISTRY_URL: $SCHEMA_REGISTRY_URL
```

---

## 🤝 Contributing

Feel free to open issues or submit pull requests for any bugs/improvements.

---

## 📄 License

This plugin is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
