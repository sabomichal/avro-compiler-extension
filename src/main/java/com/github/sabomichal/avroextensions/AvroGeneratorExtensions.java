package com.github.sabomichal.avroextensions;

import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.specific.SpecificRecord;

import java.util.*;
import java.util.stream.Collectors;

public class AvroGeneratorExtensions {

    public static final String PROP_NAME_JAVA_INTERFACE = "java-interface";
    public static final String DEFAULT_INTERFACE = SpecificRecord.class.getName();

    public String recordImplements(Schema schema) {
        var customInterfaces = javaInterfaces(schema);
        var interfaces = new ArrayList<String>(1 + customInterfaces.size());
        interfaces.add(DEFAULT_INTERFACE);
        interfaces.addAll(customInterfaces);
        return String.join(", ", interfaces);
    }

    public String javaType(SpecificCompiler delegate, Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            // get types of union and find implementing interfaces 
            var interfaces = schema.getTypes().stream()
                    .map(this::javaInterfaces)
                    .map(HashSet::new)
                    .collect(Collectors.toList());
            // find common implementing types, if any
            var commons = interfaces.stream()
                    .reduce(interfaces.get(0), (first, second) -> {
                        first.retainAll(second);
                        return first;
                    });
            if (!commons.isEmpty()) {
                return SpecificCompiler.mangleTypeIdentifier(commons.iterator().next());
            }
        }
        return delegate.javaType(schema);
    }

    public String javaUnbox(SpecificCompiler delegate, Schema schema, boolean unboxNullToVoid) {
        if (schema.getType() == Schema.Type.UNION) {
            return javaType(delegate, schema);
        }
        return delegate.javaUnbox(schema, unboxNullToVoid);
    }

    private List<String> javaInterfaces(Schema schema) {
        return Optional.ofNullable(schema.getProp(PROP_NAME_JAVA_INTERFACE))
                .map(p -> Arrays.asList(p.split("\\s,\\s")))
                .orElse(List.of());
    }
}
