package org.mapdb.elsa;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by jan on 4/17/16.
 */
public class ElsaObjectOutputStreamTest {

    @Test
    public void test_stream_override() throws IOException, ClassNotFoundException {
        ElsaSerializerPojo p = new ElsaSerializerPojo();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElsaObjectOutputStream out2 = new ElsaObjectOutputStream(out);
        out2.writeObject("aaa");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));

        assertEquals("aaa", p.deserialize(in, -1));
    }


}