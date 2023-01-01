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
        // process all union types and skip optional types (defined as union of that type and NULL type)
        if (isUnionType(schema) && !isOptionalType(schema)) {
            // get types of union and find implementing interfaces
            var interfaces = schema.getTypes().stream()
                    .filter(t -> t.getType() != Schema.Type.NULL)
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
        if (isUnionType(schema)) {
            return javaType(delegate, schema);
        }
        return delegate.javaUnbox(schema, unboxNullToVoid);
    }

    private List<String> javaInterfaces(Schema schema) {
        return Optional.ofNullable(schema.getProp(PROP_NAME_JAVA_INTERFACE))
                .map(p -> Arrays.asList(p.split("\\s,\\s")))
                .orElse(List.of());
    }

    private boolean isUnionType(Schema schema) {
        return schema.getType() == Schema.Type.UNION;
    }

    private boolean isOptionalType(Schema schema) {
        if (isUnionType(schema)) {
            var unionTypes = schema.getTypes();
            return unionTypes.size() == 2 && unionTypes.stream().anyMatch(t -> t.getType() == Schema.Type.NULL);
        } else {
            return false;
        }
    }
}
