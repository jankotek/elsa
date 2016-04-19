package examples;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaUtil;
import org.mapdb.elsa.SerializerPojo;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Serializes and deserializes data using Elsa
 */
public class Hello_World {

    @Test public void simple() throws IOException {
        //some data to be serialized
        Map data = new HashMap();
        data.put(1,"one");

        //construct Elsa serializer
        SerializerPojo serializer = new ElsaMaker().make();

        //Elsa takes DataOutput and DataInput. Construct them on top of OutputStream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);

        serializer.serialize(out2, data);

        //now deserialize data using DataInput
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));

        Map data2 = (Map) serializer.deserialize(in, -1);
        assertEquals(data, data2);
    }



}
