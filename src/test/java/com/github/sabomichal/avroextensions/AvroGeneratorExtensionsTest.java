package com.github.sabomichal.avroextensions;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class AvroGeneratorExtensionsTest {

    @Test
    public void testMarshalUnmarshal() throws IOException {
        var stateA = StateA.newBuilder()
                .setName("stateA")
                .setValue(1)
                .build();
        var recordA = Record.newBuilder()
                .setState(stateA)
                .build();

        var stateB = StateB.newBuilder()
                .setName("stateB")
                .setValue(true)
                .build();
        var recordB = Record.newBuilder()
                .setState(stateB)
                .build();

        // marshal
        var datumWriter = new SpecificDatumWriter<>(Record.class);
        var dataFileWriter = new DataFileWriter<>(datumWriter);
        var bos = new ByteArrayOutputStream();
        dataFileWriter.create(recordA.getSchema(), bos);
        dataFileWriter.append(recordA);
        dataFileWriter.append(recordB);
        dataFileWriter.close();

        // unmarshal
        var datumReader = new SpecificDatumReader<>(Record.class);
        var bis = new SeekableByteArrayInput(bos.toByteArray());

        var v = new PrintingVisitor();

        try (var dataFileReader = new DataFileReader<>(bis, datumReader)) {
            assertTrue(dataFileReader.hasNext());
            var r1 = dataFileReader.next();
            assertEquals(recordA, r1);
            r1.getState().accept(v);

            assertTrue(dataFileReader.hasNext());
            var r2 = dataFileReader.next();
            assertEquals(recordB, r2);
            r2.getState().accept(v);

            assertFalse(dataFileReader.hasNext());
        }
    }

    private class PrintingVisitor implements Visitor {

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
