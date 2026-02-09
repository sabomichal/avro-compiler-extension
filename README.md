[![Maven Central](https://img.shields.io/maven-central/v/com.github.sabomichal/avro-compiler-extension)](https://central.sonatype.com/artifact/com.github.sabomichal/avro-compiler-extension)
![Java CI with Maven](https://github.com/sabomichal/avro-compiler-extension/workflows/Java%20CI%20with%20Maven/badge.svg)
## avro-compiler-extension
avro-compiler-extension is an Avro IDL to Java _SpecificCompiler_ extension adding custom IDL annotations, specifically:

* **@java-interface** type annotation, making given record implement the specified Java interface
* **@java-final** type annotation, making given record a final Java class
* generated UNION types are no longer automatically generated with Java Object type, but the UNION types common Java interface  

### Avro version
Extension is compatible with Avro 1.12.x

### Java version
Target Java versions is 21

### Annotations provided by the extension
The extension provides following custom annotations. Please see the examples for further information.

#### @java-interface
The '@java-interface' annotation makes the given record implement the comma separated list of interfaces. 
```idl
  @java-interface("com.example.State")
  record StateA {
    string name;
  }
```

#### @java-final
The '@java-final' annotation makes the given record a final class. This is particularly useful when used in conjunction with sealed interfaces.
```idl
  @java-final("")
  record StateA {
    string name;
  }
```

### Usage
#### Maven
Maven users simply add the avro-compiler-extension extension as a dependency to avro-maven-plugin and set new Velocity template directory and tools class. The following example demonstrates the usage.
```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>${avro.version}</version>
    <dependencies>
        <dependency>
            <groupId>com.github.sabomichal</groupId>
            <artifactId>avro-compiler-extension</artifactId>
            <version>${avro.compiler.extension.version}</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <goals>
                <goal>idl-protocol</goal>
            </goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/avro/</sourceDirectory>
                <templateDirectory>${project.basedir}/src/main/resources/velocity/</templateDirectory>
                <velocityToolsClassesNames>com.github.sabomichal.avroextensions.AvroGeneratorExtensions</velocityToolsClassesNames>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Release notes
#### 1.0
* Avro 1.12.x support
* Java 21 min compatibility
#### 0.1
* first initial version

If you like it, give it a star, if you don't, please write an issue.
