package doc;

import org.junit.Test;

/**
 * Simples use of Elsa serialization
 */
public class ref_1 {

    @Test
    public void reference() throws Exception {
        //#a
        Long a = new Long(100);
        // create object graph with duplicate references
        Object[] array1 = new Object[]{a, a};

        //object graph with circular reference, array2 references itself
        Object[] array2 = new Object[]{a, null};
        array2[1] = array2;
        //#z
    }
}
