package org.mapdb.elsa;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class ElsaObjectInputStreamTest {

    @Test public void test_stream_override() throws IOException, ClassNotFoundException {
        SerializerPojo p = new SerializerPojo();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);
        p.serialize(out2, "aaa");

        ElsaObjectInputStream in = new ElsaObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("aaa", in.readObject());
    }

}