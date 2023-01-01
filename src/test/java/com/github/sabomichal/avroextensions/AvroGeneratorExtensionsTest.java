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

        var stateB = StateB.newBuilder()
                .setName("stateB")
                .setValue(true)
                .build();

        var recordA1 = RecordA.newBuilder()
                .setState(stateA)
                .setErrorType(null)
                .build();

        var recordA2 = RecordA.newBuilder()
                .setState(stateB)
                .setErrorTypeBuilder(Error.newBuilder().setMessage("message"))
                .build();

        var recordB = RecordB.newBuilder()
                .setState(null)
                .setErrorTypeBuilder(Error.newBuilder().setMessage("message"))
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
        var datumReader = new SpecificDatumReader<>(RecordA.class);
        var bis = new SeekableByteArrayInput(bos.toByteArray());

        var v = new PrintingVisitor();

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
