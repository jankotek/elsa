package examples;

import org.junit.Test;
import org.mapdb.elsa.*;

import java.io.*;

import static org.junit.Assert.assertEquals;

/**
 * Serializes and deserializes data using Elsa
 */
public class Hello_World {

    @Test public void simple() throws IOException {
        //some data to be serialized
        String data = "Hello World";

        //construct Elsa serializer
        ElsaSerializerPojo serializer = new ElsaMaker().make();

        //Elsa takes DataOutput and DataInput. Construct them on top of OutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);

        serializer.serialize(out2, data);

        //now deserialize data using DataInput
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));

        String data2 = (String)serializer.deserialize(in);
        assertEquals(data, data2);
    }



}
