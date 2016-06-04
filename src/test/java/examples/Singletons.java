package examples;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializerPojo;
import org.mapdb.elsa.ElsaUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Shows howto use singletons in Elsa
 */
public class Singletons {

    @Test
    public void singletons() throws IOException {
        // Initialize data for serialization.
        // Current thread is not serializable and is configured as singleton
        Map data = new HashMap();
        data.put("thread", Thread.currentThread());

        // initialize serializer
        ElsaSerializerPojo s = new ElsaMaker()
                .singletons(Thread.currentThread())  // Current thread is singleton
                .make();

        // data can be serialized using s.serialize.. methods
        // in this case we just do binary clone
        Map data2 = ElsaUtil.clone(s, data);

        // singleton is restored and reference equality is preserved
        assertTrue(data2.get("thread") == Thread.currentThread());

        assertTrue(data != data2);  // reference equality after binary clone is not preserved
        assertEquals(data, data2);  // but serialized objects are equal

    }
}
