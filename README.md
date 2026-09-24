<div align="center">

# avro-compiler-extension

**Type-safe Avro unions for Java.**

An extension for Avro's Java `SpecificCompiler` that types `union` fields as a shared
interface instead of `Object` — so a Java 21 `switch` over a union is checked for
exhaustiveness at compile time.

[![Maven Central](https://img.shields.io/maven-central/v/com.github.sabomichal/avro-compiler-extension?logo=apachemaven&label=Maven%20Central)](https://central.sonatype.com/artifact/com.github.sabomichal/avro-compiler-extension)
[![Java CI with Maven](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/)
[![Apache Avro](https://img.shields.io/badge/Apache%20Avro-1.12.x-1BA0E2?logo=apache&logoColor=white)](https://avro.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.txt)

[Why](#why) · [Quick start](#quick-start) · [Example](#example) · [Type mapping](#type-mapping) · [Limitations](#limitations) · [Releases](https://github.com/sabomichal/avro-compiler-extension/releases)

</div>

---

## Why

Stock Avro generates `java.lang.Object` for every `union { A, B }` field. The compiler can't
help you, so you write `instanceof` chains — and a forgotten branch only shows up at runtime.

<table>
<tr><th>❌ Stock Avro</th><th>✅ With this extension</th></tr>
<tr><td>

```java
Object state = record.getState();

if (state instanceof StateA a) {
    handle(a);
} else if (state instanceof StateB b) {
    handle(b);
}
// new StateC? still compiles
```

</td><td>

```java
State state = record.getState();

switch (state) {
    case StateA a -> handle(a);
    case StateB b -> handle(b);
}
// new StateC? compile error
```

</td></tr>
</table>

What the extension adds:

- **Typed unions** — if every member of a union implements the same interface, the field,
  getter, setter and builder use that interface. Works for plain unions, nullable unions and
  `array<union>`.
- **`@java-interface`** — the generated record implements the listed Java interfaces.
- **`@java-final`** — the generated record is a `final` class, so it can be a permitted
  subtype of a `sealed` interface.

Unions whose members share no interface are generated exactly as stock Avro would.

## Quick start

Two things have to be wired into the Avro compiler:

1. the extension jar on the **compiler's** classpath — it carries both the Velocity tool class
   and the replacement `velocity/*.vm` templates;
2. `templateDirectory = velocity/` plus the `AvroGeneratorExtensions` tool class.

### Maven

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.12.1</version>
    <dependencies>
        <dependency>
            <groupId>com.github.sabomichal</groupId>
            <artifactId>avro-compiler-extension</artifactId>
            <version>1.2</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>idl-protocol</goal>
            </goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/avro/</sourceDirectory>
                <templateDirectory>velocity/</templateDirectory>
                <velocityToolsClassesNames>com.github.sabomichal.avroextensions.AvroGeneratorExtensions</velocityToolsClassesNames>
                <stringType>String</stringType>
                <fieldVisibility>private</fieldVisibility>
                <gettersReturnOptional>true</gettersReturnOptional>
                <optionalGettersForNullableFieldsOnly>true</optionalGettersForNullableFieldsOnly>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle

With [gradle-avro-plugin](https://github.com/davidmc24/gradle-avro-plugin), put the extension
on the **`buildscript` classpath** — the Gradle counterpart of the plugin `<dependencies>`
block above:

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.apache.avro:avro-compiler:1.12.1"   // required, see note
        classpath "com.github.sabomichal:avro-compiler-extension:1.2"
    }
}

plugins {
    id "java"
    id "com.github.davidmc24.gradle.plugin.avro" version "1.9.1"
}

avro {
    templateDirectory = "velocity/"
    additionalVelocityToolClasses = ["com.github.sabomichal.avroextensions.AvroGeneratorExtensions"]
    stringType = "String"
    fieldVisibility = "PRIVATE"
    gettersReturnOptional = true
    optionalGettersForNullableFieldsOnly = true
}
```

> [!IMPORTANT]
> **Pin `avro-compiler` to `1.12.x`.** gradle-avro-plugin ships with an older Avro whose
> `SpecificCompiler` lacks methods the templates call. Without the pin, generation fails with
> `SpecificCompiler does not contain method getSchemaParentClass`.
> Maven needs no pin — `avro-maven-plugin` 1.12.1 already brings Avro 1.12.1.

<details>
<summary><b>Troubleshooting</b></summary>

<br>

| Symptom | Cause |
|---------|-------|
| `unable to load velocity tool class …` | The extension jar isn't on the compiler's classpath. In Gradle it must be a `buildscript` `classpath` dependency — a regular `implementation`/`compileOnly` dependency is invisible to the plugin. |
| Unions are still generated as `Object` | `templateDirectory` is missing. The tool class alone does nothing; only the bundled templates call it. |
| `does not contain method getSchemaParentClass` | Avro older than 1.12 on the compiler classpath — see the pin above. |

</details>

## Example

**1. Avro IDL** — both states are `final` and implement the same interface:

```idl
@namespace("com.example")
protocol Simple {

  @java-final("")
  @java-interface("com.example.State")
  record StateA {
    string name;
    long value;
  }

  @java-final("")
  @java-interface("com.example.State")
  record StateB {
    string name;
    boolean value;
  }

  record RecordA {
    union { StateA, StateB } state;
  }
}
```

**2. The interface** — written by hand, `sealed` over exactly the generated records:

```java
package com.example;

public sealed interface State permits StateA, StateB {
}
```

**3. Use it** — `RecordA.getState()` returns `State`, and the `switch` needs no `default`:

```java
static String describe(RecordA record) {
    return switch (record.getState()) {
        case StateA a -> "A: " + a.getName();
        case StateB b -> "B: " + b.getName();
    };
}
```

> [!TIP]
> Prefer a visitor? [`src/test/java/com/example`](src/test/java/com/example) shows the same
> union dispatched through `State#accept(Visitor)`.

## Type mapping

| Avro IDL field | Generated getter |
|----------------|------------------|
| `union { StateA, StateB } state` | `State getState()` |
| `union { null, StateA, StateB } state` | `Optional<State> getState()` <sup>1</sup> |
| `array<union { StateA, StateB }> states` | `List<State> getStates()` |
| `MessagePayload? payload` | `Optional<MessagePayload> getPayload()` <sup>1</sup> |
| union members without a shared interface | `Object` — unchanged |

<sup>1</sup> `Optional` comes from Avro's `gettersReturnOptional` +
`optionalGettersForNullableFieldsOnly`. Without them the getter returns the plain type
(`State`, `MessagePayload`).

## Annotations

| Annotation | Applies to | Effect |
|------------|------------|--------|
| `@java-interface("a.B, c.D")` | record | Record implements the listed interfaces (comma-separated), in addition to `SpecificRecord`. |
| `@java-final("")` | record | Record is generated as a `final` class. The value is ignored. |

## Limitations

- **Maps are not covered.** `map<union { … }>` is still generated as `Map<String, Object>`.
- **Declare exactly one shared interface.** If union members share several `@java-interface`
  entries, which one becomes the field type is not defined.
- **The interface is yours.** The extension only references it; you write it (and make it
  `sealed` if you want exhaustive `switch`).

## License

[MIT](LICENSE.txt)

---

<div align="center">

If it helps you, give it a ⭐ — if it doesn't, [open an issue](https://github.com/sabomichal/avro-compiler-extension/issues).

</div>
