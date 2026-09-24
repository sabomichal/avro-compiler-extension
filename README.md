<div align="center">

# avro-compiler-extension

**Type-safe Avro unions for Java.**
An Avro IDL → Java `SpecificCompiler` extension that maps `union` types to a common
interface instead of raw `Object` — so you get exhaustive `switch` pattern matching
with compile-time guarantees.

[![Maven Central](https://img.shields.io/maven-central/v/com.github.sabomichal/avro-compiler-extension?logo=apachemaven&label=Maven%20Central)](https://central.sonatype.com/artifact/com.github.sabomichal/avro-compiler-extension)
[![Java CI with Maven](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/sabomichal/avro-compiler-extension/actions/workflows/maven.yml)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/)
[![Apache Avro](https://img.shields.io/badge/Apache%20Avro-1.12.x-1BA0E2?logo=apache&logoColor=white)](https://avro.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.txt)

</div>

---

## Why?

By default, Avro generates a raw `java.lang.Object` field for every `union { A, B }`. You
lose all compile-time type information and end up writing `instanceof` chains and casts:

```java
// ❌ stock Avro — the union is an Object
Object state = record.getState();
if (state instanceof StateA a) {
    handle(a);
} else if (state instanceof StateB b) {
    handle(b);
}
// forgot a member? Still compiles. Blows up at runtime.
```

This extension maps the union members to a **common interface** they all declare. Pair it
with a Java 21 [`sealed interface`](https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html)
and the union becomes fully typed and **exhaustively matchable**:

```java
// ✅ with avro-compiler-extension — the union is a typed, sealed interface
State state = record.getState();
return switch (state) {          // exhaustive — no default branch needed
    case StateA a -> handle(a);
    case StateB b -> handle(b);
};
// add StateC to the sealed interface → this stops compiling until you handle it
```

## Features

- 🎯 **Typed unions** — a `union` whose members share a common interface is generated with
  that interface as its Java type instead of `Object` (works for plain unions, `array<union>`
  and optional unions).
- 🧩 **`@java-interface`** — make a record implement one or more Java interfaces.
- 🔒 **`@java-final`** — make a record a `final` class, so it can be a permitted subtype of a
  `sealed` interface.
- ♻️ **Safe fallback** — unions without a shared interface generate exactly as stock Avro does.

## Compatibility

| Dependency | Version |
|------------|---------|
| Apache Avro | `1.12.x` |
| Java        | `21+`   |

## Type mapping

When every non-`null` member of a union declares a common `@java-interface`, the compiler
uses that interface as the field's Java type:

| Avro IDL field | Generated Java accessor |
|----------------|-------------------------|
| `union { StateA, StateB } state` | `State getState()` |
| `array<union { StateA, StateB }> states` | `List<State> getStates()` |
| `union { null, StateA, StateB } state` | `Optional<State> getState()` <sup>†</sup> |
| `MessagePayload? payload` | `Optional<MessagePayload> getPayload()` <sup>†</sup> |

<sup>†</sup> The `Optional<…>` wrapping comes from Avro's own
`gettersReturnOptional` + `optionalGettersForNullableFieldsOnly` options. Without those flags
the field is still typed as the common interface, just not wrapped. If the union members do
**not** share a common interface, generation falls back to Avro's default `Object`.

## Annotations

<table>
<tr><th><code>@java-interface</code></th><th><code>@java-final</code></th></tr>
<tr><td>

Makes the record implement a comma-separated list of interfaces.

```idl
@java-interface("com.example.State")
record StateA {
  string name;
  long value;
}
```

</td><td>

Makes the record a `final` class (required for a `sealed` permitted subtype). The annotation
value is ignored — pass `""`.

```idl
@java-final("")
record StateA {
  string name;
  long value;
}
```

</td></tr>
</table>

## Quick start

Both build tools need the same two things wired up:

1. the extension jar on the **compiler's** classpath, so it can load the Velocity tool class
   **and** the bundled `velocity/*.vm` templates, and
2. the custom `templateDirectory` (`velocity/`) plus the `AvroGeneratorExtensions` tool class.

### Maven

Add the extension as a dependency of `avro-maven-plugin` and point the plugin at the custom
template directory and tool class:

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

With the [gradle-avro-plugin](https://github.com/davidmc24/gradle-avro-plugin), add the
extension to the **`buildscript` classpath** — the same classloader that loads the plugin,
Avro and Velocity. This is the Gradle equivalent of putting it inside `avro-maven-plugin`'s
`<dependencies>`, and it is what makes both the tool class and the `velocity/*.vm` templates
resolvable.

```gradle
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath "com.github.davidmc24.gradle.plugin:gradle-avro-plugin:1.9.1"
        // Pin Avro 1.12.x on the buildscript classpath (see note below).
        classpath "org.apache.avro:avro-compiler:1.12.1"
        classpath "com.github.sabomichal:avro-compiler-extension:1.2"
    }
}

apply plugin: "java"
apply plugin: "com.github.davidmc24.gradle.plugin.avro"

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
> **Pin `avro-compiler` to `1.12.x` on the `buildscript` classpath.** `gradle-avro-plugin`
> bundles its own, older Avro whose `SpecificCompiler` is missing methods the `1.12.x`
> templates call. Without the pin, generation fails with
> `Object 'org.apache.avro.compiler.specific.SpecificCompiler' does not contain method getSchemaParentClass`.
> (Maven doesn't need this — its plugin version already selects Avro `1.12.x`.)

<details>
<summary><b>Gradle pitfalls</b> — why <code>buildscript { }</code> and not <code>plugins { }</code></summary>

<br>

The plugin loads Velocity tool classes and the custom templates from the same classloader it
runs in, so the extension jar has to be on that classloader:

- `avro { dependencies { compileOnly '…' } }` is **not** valid plugin configuration — the jar
  never reaches the compiler, which is why you get *"unable to load velocity tool class …"*.
- `templateDirectory = "velocity/"` is required even once the class loads; without the bundled
  templates the tool is never invoked.
- Prefer the `buildscript { }` + `apply plugin:` form over the `plugins { }` block: a plugin
  applied via `plugins { }` runs in an isolated classloader that the extension jar cannot be
  added to, so the `velocity/*.vm` resources would not resolve.

</details>

## Example: sealed types + exhaustive union switch

This is the whole point of the extension. Two states can travel in the same field, and the
Java compiler forces you to handle both.

**1. The Avro IDL** — both records are `final` and implement the same interface:

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

  @java-interface("com.example.Payload")
  record MessagePayload {
    string message;
  }

  record RecordA {
    union { StateA, StateB } state;
    MessagePayload? payload;
  }
}
```

**2. The hand-written interface** — a Java 21 `sealed interface` that permits exactly the
generated records:

```java
package com.example;

public sealed interface State permits StateA, StateB {
}
```

**3. The generated accessors** — thanks to the extension, `RecordA` exposes `State`, not
`Object` (and `Optional<MessagePayload>` for the nullable field):

```java
State getState();                       // not Object
Optional<MessagePayload> getPayload();  // optional getters enabled
```

**4. The payoff** — an exhaustive `switch` with pattern matching and **no `default` branch**.
The compiler verifies every permitted subtype is covered; add `StateC` to the `sealed`
interface and this stops compiling until you handle it:

```java
static String describe(State state) {
    return switch (state) {          // exhaustive — no default needed
        case StateA a -> "A: " + a.getName();
        case StateB b -> "B: " + b.getName();
    };
}
```

> [!TIP]
> A visitor-style alternative (`State#accept(Visitor)`) is also possible and is shown in
> [`src/test/java/com/example`](src/test/java/com/example); exhaustive `switch` is the more
> concise Java 21 idiom.

## Release notes

<details open>
<summary><b>Version history</b></summary>

<br>

| Version | Changes |
|---------|---------|
| **1.2** | Correct Java unbox handling; fixed non-union array types |
| **1.1** | Coverage for `array<union>` types; correct `templateDirectory` usage |
| **1.0** | Avro 1.12.x support; Java 21 minimum |
| **0.1** | First initial version |

</details>

## License

Released under the [MIT License](LICENSE.txt).

---

<div align="center">

If you like it, give it a ⭐ — if you don't, please [open an issue](https://github.com/sabomichal/avro-compiler-extension/issues).

</div>
