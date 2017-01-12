package doc;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;

/**
 * Simples use of Elsa serialization
 */
public class ref_2 {

    @Test
    public void reference() throws Exception {
        //#a
        ElsaSerializer serializer =
                new ElsaMaker()
                .referenceHashMapEnable()
                .make();
        //#z
    }
}
