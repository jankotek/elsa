package doc;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;

/**
 * Simples use of Elsa serialization
 */
public class option_class_loader {

    @Test
    public void hello() throws Exception {
        //#a
        ClassLoader myClassLoader =
                Thread.currentThread().getContextClassLoader();

        ElsaSerializer serializer =
                new ElsaMaker()
                .classLoader(myClassLoader)
                .make();
        //#z
    }
}
