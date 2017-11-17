package examples;

import org.junit.*;
import org.mapdb.elsa.*;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Shows various option to handle references, cyclic references,
 * duplicate references and deduplication.
 */
public class References {

    @Test @Ignore
    public void noReferenceHandling() throws IOException {
        String object = "object";
        //pair of two objects
        Object[] obj = new Object[]{object, object};

        ElsaSerializer serializer = new ElsaMaker()
                //disable reference tracking
                .referenceDisable()
                .make();

        Object[] cloned = serializer.clone(obj);

        // object identity is not preserved after cloning
        assertTrue(cloned[0]!=cloned[1]);

        //demonstrate cyclic reference
        obj[0] = obj;
        try {
            serializer.clone(obj);
            fail("should throw an exception");
        }catch(java.lang.OutOfMemoryError e){
            //FIXME this is just nasty, add limit on stack size?
            //expected
        }
    }


    @Test
    public void arrayReferenceHandling() throws IOException {
        String object = "object";
        //pair of two objects
        Object[] obj = new Object[]{object, object};

        ElsaSerializer serializer = new ElsaMaker()
                // Enable reference array, slower on larger graph,
                // but faster on very small graphs.
                .referenceArrayEnable()
                .make();

        Object[] cloned = serializer.clone(obj);

        // object identity is preserved after cloning
        assertTrue(cloned[0]==cloned[1]);
    }


    @Test
    public void deduplication() throws IOException {
        Integer object1 = new Integer(10000);
        Integer object2 = new Integer(10000);
        //pair of two objects
        Object[] obj = new Object[]{object1, object2};

        ElsaSerializer serializer = new ElsaMaker()
                // Use HashMap to track reference,
                // but also to deduplicate different objects which are equal
                .referenceHashMapEnable()
                .make();

        Object[] cloned = serializer.clone(obj);

        // equal but not-identical objects are identical after cloning
        assertTrue(cloned[0]==cloned[1]);
    }
}
