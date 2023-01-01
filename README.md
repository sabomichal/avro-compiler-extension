[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.sabomichal/immutable-xjc-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.sabomichal/immutable-xjc-plugin) ![Java CI with Maven](https://github.com/sabomichal/immutable-xjc/workflows/Java%20CI%20with%20Maven/badge.svg)
## immutable-xjc
Avroextensions is an _avro-maven-plugin_ plugin extension adding custom IDL annotations, specifically:

* @java-interface type annotation, making given 

### Avro version
Plugin is built against Avro 1.11.1

### Java version
Target Java versions is 11

### Options provided by the extension
The plugin provides an '-immutable' option which is enabled by adding its jar file to the XJC classpath. When enabled, additional options can be used to control the behavior of the plugin. See the examples for further information.

#### -immutable
The '-immutable' option enables the plugin making the XJC generated classes immutable.

### Usage
#### Maven
Maven users simply add the IMMUTABLE-XJC plugin as a dependency to a JAXB plugin of choice. The following example demonstrates the use of the IMMUTABLE-XJC plugin with the mojo *https://github.com/evolvedbinary/jvnet-jaxb-maven-plugin*.
```xml
<plugin>
    <groupId>com.evolvedbinary.maven.jvnet</groupId>
    <artifactId>jaxb30-maven-plugin</artifactId>
    <version>0.15.0</version>
    <dependencies>
        <dependency>
            <groupId>com.github.sabomichal</groupId>
            <artifactId>immutable-xjc-plugin</artifactId>
            <version>1.8.0</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <specVersion>4.0.0</specVersion>
                <args>
                    <arg>-immutable</arg>
                    <arg>-imm-builder</arg>
                </args>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Release notes
#### 0.1
* builder class now contains initialised collection fields

If you like it, give it a star, if you don't, write an issue.
