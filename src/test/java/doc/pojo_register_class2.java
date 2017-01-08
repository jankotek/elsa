package doc;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;
import org.mapdb.elsa.ElsaUtil;

import java.util.ArrayList;
import java.util.List;

public class pojo_register_class2 {

    @Test
    public void register_class(){
        //#a
        List somePojos = new ArrayList();
        somePojos.add(new SomePojo());

        // traverse Object Graph and return all unknown classes
        Class[] classes = ElsaUtil.findUnknownClasses(somePojos);

        // register unknown classes with serializer
        ElsaSerializer ser = new ElsaMaker()
                .registerClasses(classes)
                .make();
        //#z
    }


}
