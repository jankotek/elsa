package org.mapdb.elsa;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ElsaMakerTest {

    Class o;

    @Test
    public void unknownClass() throws IOException {
        ClassCallback callback = new ClassCallback() {
            @Override
            public void classMissing(Class clazz) {
                o = clazz;
            }
        };
        SerializerPojo s = new ElsaMaker().unknownClassNotification(callback).make();
        SerializerBaseTest.clonePojo(new Serialization2Bean(), s);
        assertEquals(Serialization2Bean.class, o);
    }

}
