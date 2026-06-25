package com.github.sabomichal.avroextensions;

import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.specific.SpecificRecord;

import java.util.*;

public class AvroGeneratorExtensions {

    public static final String PROP_NAME_JAVA_INTERFACE = "java-interface";
    public static final String PROP_NAME_JAVA_FINAL = "java-final";

    public static final String DEFAULT_INTERFACE = SpecificRecord.class.getName();

    public String recordImplements(Schema schema) {
        var customInterfaces = javaInterfaces(schema);
        var interfaces = new ArrayList<String>(1 + customInterfaces.size());
        interfaces.add(DEFAULT_INTERFACE);
        interfaces.addAll(customInterfaces);
        return String.join(", ", interfaces);
    }

    public boolean recordFinal(Schema schema) {
        return javaFinal(schema);
    }

    public String javaType(SpecificCompiler delegate, Schema schema) {
        // process all union types and skip optional types (defined as union of that type and NULL type)
        if (isUnionType(schema) && !isOptionalType(schema)) {
            var types = schema.getTypes();
            // get types of union and find implementing interfaces
            var commons = findCommonImplementingType(types);
            if (!commons.isEmpty()) {
                return SpecificCompiler.mangleTypeIdentifier(commons.iterator().next());
            }
        } else if (isArrayType(schema) && isUnionType(schema.getElementType())) {
            var types = schema.getElementType().getTypes();
            // get types of union and find implementing interfaces
            var commons = findCommonImplementingType(types);
            if (!commons.isEmpty()) {
                return "java.util.List<" + SpecificCompiler.mangleTypeIdentifier(commons.iterator().next()) + ">";
            }
        }
        return delegate.javaType(schema);
    }

    private HashSet<String> findCommonImplementingType(List<Schema> types) {
        var interfaces = types.stream()
                .filter(t -> t.getType() != Schema.Type.NULL)
                .map(this::javaInterfaces)
                .map(HashSet::new)
                .toList();
        if (interfaces.isEmpty()) {
            return new HashSet<>();
        }
        // find common implementing types, if any
        return interfaces.stream()
                .reduce(interfaces.getFirst(), (first, second) -> {
                    first.retainAll(second);
                    return first;
                });
    }

    public String javaUnbox(SpecificCompiler delegate, Schema schema, boolean unboxNullToVoid) {
        switch (schema.getType()) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                // let the delegate handle unboxing of primitive types
                return delegate.javaUnbox(schema, unboxNullToVoid);
            case NULL:
                if (unboxNullToVoid) {
                    return delegate.javaUnbox(schema, true);
                }
                // fall through
            default:
                // not unboxed: cover the default with our javaType, not the delegate one
                return javaType(delegate, schema);
        }
    }

    private List<String> javaInterfaces(Schema schema) {
        return Optional.ofNullable(schema.getProp(PROP_NAME_JAVA_INTERFACE))
                .map(p -> Arrays.asList(p.split("\\s*,\\s*")))
                .orElse(List.of());
    }

    private boolean javaFinal(Schema schema) {
        return Optional.ofNullable(schema.getProp(PROP_NAME_JAVA_FINAL))
                .isPresent();
    }

    private boolean isUnionType(Schema schema) {
        return schema.getType() == Schema.Type.UNION;
    }

    private boolean isArrayType(Schema schema) {
        return schema.getType() == Schema.Type.ARRAY;
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
