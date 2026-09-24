package com.github.sabomichal.avroextensions;

import com.example.*;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AvroGeneratorExtensionsTest {

    @Test
    public void testMarshalUnmarshal() throws IOException {
        State stateA = StateA.newBuilder()
                .setName("stateA")
                .setValue(1)
                .build();

        State stateB = StateB.newBuilder()
                .setName("stateB")
                .setValue(true)
                .build();

        var recordA1 = RecordA.newBuilder()
                .setState(stateA)
                .setPayload(null)
                .build();

        var recordA2 = RecordA.newBuilder()
                .setState(stateB)
                .setPayload(MessagePayload.newBuilder()
                        .setMessage("message")
                        .build())
                .build();

        // marshal
        var datumWriter = new SpecificDatumWriter<>(RecordA.class);
        var dataFileWriter = new DataFileWriter<>(datumWriter);
        var bos = new ByteArrayOutputStream();
        dataFileWriter.create(recordA1.getSchema(), bos);
        dataFileWriter.append(recordA1);
        dataFileWriter.append(recordA2);
        dataFileWriter.close();

        // unmarshal
        var v = new PrintingVisitor();
        var datumReader = new SpecificDatumReader<>(RecordA.class);
        var bis = new SeekableByteArrayInput(bos.toByteArray());

        try (var dataFileReader = new DataFileReader<>(bis, datumReader)) {
            assertTrue(dataFileReader.hasNext());
            var r1 = dataFileReader.next();
            assertEquals(recordA1, r1);
            r1.getState().accept(v);

            assertTrue(dataFileReader.hasNext());
            var r2 = dataFileReader.next();
            assertEquals(recordA2, r2);
            r2.getState().accept(v);

            assertFalse(dataFileReader.hasNext());
        }
    }

    @Test
    public void testOptionalGettersSettersCorrectType() {
        State state = StateA.newBuilder()
                .setName("state")
                .setValue(1)
                .build();

        var recordB1 = RecordB.newBuilder()
                .setState(null)
                .setPayloadBuilder(MessagePayload.newBuilder().setMessage("message"))
                .build();
        Optional<State> expectedB1 = Optional.empty();
        assertEquals(expectedB1, recordB1.getState());

        var recordB2 = RecordB.newBuilder()
                .setState(state)
                .setPayloadBuilder(MessagePayload.newBuilder().setMessage("message"))
                .build();
        Optional<State> expectedB2 = Optional.of(state);
        assertEquals(expectedB2, recordB2.getState());
    }

    @Test
    public void testSeveralSharedInterfacesPickFirstDeclared() {
        var a = record("A", "com.example.Zeta, com.example.Alpha");
        var b = record("B", "com.example.Alpha, com.example.Zeta");
        var union = Schema.createUnion(a, b);
        var extensions = new AvroGeneratorExtensions();
        var compiler = new SpecificCompiler(union);

        assertEquals("com.example.Zeta", extensions.javaType(compiler, union));
        assertEquals("java.util.List<com.example.Zeta>", extensions.javaType(compiler, Schema.createArray(union)));
    }

    private static Schema record(String name, String interfaces) {
        var schema = Schema.createRecord(name, null, "com.example", false, List.of());
        schema.addProp(AvroGeneratorExtensions.PROP_NAME_JAVA_INTERFACE, interfaces);
        return schema;
    }

    private static class PrintingVisitor implements Visitor {

        @Override
        public void visit(StateA state) {
            System.out.println("State A");
        }

        @Override
        public void visit(StateB state) {
            System.out.println("State B");
        }
    }
}
