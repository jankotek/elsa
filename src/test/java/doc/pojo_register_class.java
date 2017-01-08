package doc;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;

public class pojo_register_class {

    @Test
    public void register_class(){
        //#a
        ElsaSerializer ser = new ElsaMaker()
                .registerClasses(SomePojo.class)
                .make();
        //#z
    }


}
