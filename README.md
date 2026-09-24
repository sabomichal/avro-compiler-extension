# avro-compiler-extension

[![Maven Central](https://img.shields.io/maven-central/v/com.github.sabomichal/avro-compiler-extension?label=Maven%20Central)](https://central.sonatype.com/artifact/com.github.sabomichal/avro-compiler-extension)
[![Build](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml)

Type-safe Avro unions for Java 21+. Union fields get a shared interface type instead of
`Object`, so a `switch` over them is checked for exhaustiveness at compile time.

```diff
- Object state = record.getState();
- if (state instanceof StateA a) { … } else if (state instanceof StateB b) { … }
+ State state = record.getState();
+ switch (state) {
+     case StateA a -> handle(a);
+     case StateB b -> handle(b);
+ }
```

## Features

- **Typed unions**: if all union members implement the same interface, the field, getter,
  setter and builder use it. Works for plain unions, nullable unions and `array<union>`.
  With several shared interfaces, the first one listed on the first member wins.
- **`@java-interface("a.B, c.D")`**: the record implements the listed interfaces.
- **`@java-final("")`**: the record is a `final` class, so it can be permitted by a `sealed` interface.

Unions without a shared interface are generated as stock Avro does.

## Setup

The extension version matches the Avro version it targets (extension `1.12.2` = Avro `1.12.2`);
a `-N` suffix marks fix releases for the same Avro version (`1.12.2-1`).

### Maven

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.12.2</version>
    <dependencies>
        <dependency>
            <groupId>com.github.sabomichal</groupId>
            <artifactId>avro-compiler-extension</artifactId>
            <version>1.12.2-1</version>
        </dependency>
    </dependencies>
    <configuration>
        <templateDirectory>velocity/</templateDirectory>
        <velocityToolsClassesNames>com.github.sabomichal.avroextensions.AvroGeneratorExtensions</velocityToolsClassesNames>
    </configuration>
</plugin>
```

### Gradle

With [gradle-avro-plugin](https://github.com/davidmc24/gradle-avro-plugin). The extension
goes on the `buildscript` classpath, and `avro-compiler` must be pinned (the plugin
bundles an older Avro).

```groovy
buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath "org.apache.avro:avro-compiler:1.12.2"
        classpath "com.github.sabomichal:avro-compiler-extension:1.12.2-1"
    }
}

avro {
    templateDirectory = "velocity/"
    additionalVelocityToolClasses = ["com.github.sabomichal.avroextensions.AvroGeneratorExtensions"]
}
```

Tip: `gettersReturnOptional` + `optionalGettersForNullableFieldsOnly` make nullable unions
return `Optional<State>`.

### Avro 1.12.2+ class allowlist

Avro 1.12.2 only deserializes classes from trusted packages
([AVRO-4189](https://github.com/apache/avro/releases/tag/release-1.12.2)). Set this on the
JVM command line:

```shell
-Dorg.apache.avro.SERIALIZABLE_PACKAGES=com.example
```

## Example

```java
@namespace("com.example")
protocol Simple {
  @java-final("")
  @java-interface("com.example.State")
  record StateA { string name; }

  @java-final("")
  @java-interface("com.example.State")
  record StateB { string name; }

  record RecordA { union { StateA, StateB } state; }
}
```

Write the interface yourself:

```java
public sealed interface State permits StateA, StateB {}
```

Then `RecordA.getState()` returns `State`:

```java
return switch (record.getState()) {
    case StateA a -> "A: " + a.getName();
    case StateB b -> "B: " + b.getName();
};
```

A visitor-based variant is in [`src/test/java/com/example`](src/test/java/com/example).

## Type mapping

| Avro IDL                                 | Getter              |
|------------------------------------------|---------------------|
| `union { StateA, StateB }`               | `State`             |
| `union { null, StateA, StateB }`         | `Optional<State>` * |
| `array<union { StateA, StateB }>`        | `List<State>`       |
| members without a shared interface       | `Object`            |

\* with `gettersReturnOptional` + `optionalGettersForNullableFieldsOnly`

## Troubleshooting

| Error                                             | Fix                                                        |
|---------------------------------------------------|------------------------------------------------------------|
| `unable to load velocity tool class`              | Extension jar not on the compiler classpath (Gradle: `buildscript`). |
| Unions still `Object`                             | `templateDirectory` is missing.                            |
| `does not contain method getSchemaParentClass`    | Pin `avro-compiler` to 1.12+.                              |
| `SecurityException: Forbidden … not trusted`      | See [class allowlist](#avro-1122-class-allowlist).         |

## Limitations

- `map<union { … }>` is still `Map<String, Object>`.

## License

[MIT](LICENSE.txt)
