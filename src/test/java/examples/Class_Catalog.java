package examples;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializerPojo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import static org.junit.Assert.assertTrue;

/**
 * Shows how class catalog can reduce data usage
 */
public class Class_Catalog {

    /** this class is serialized using object graph */
    public static class Bean implements Serializable {
        final int someData;

        public Bean(int someData) {
            this.someData = someData;
        }
    }

    @Test
    public void withCatalog() throws IOException {
        //data for serialization
        Object data = new Bean(11);

        //serialize without using class catalog
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);

        ElsaSerializerPojo serializer = new ElsaMaker().make();

        serializer.serialize(out2, data);
        int sizeWithoutCatalog = out.toByteArray().length;

        //serialize with using class catalog
        out = new ByteArrayOutputStream();
        out2 = new DataOutputStream(out);

        serializer = new ElsaMaker()
                //this registers Bean class into class catalog
                .registerClasses(Bean.class)
                .make();

        serializer.serialize(out2, data);
        int sizeWithCatalog = out.toByteArray().length;

        System.out.println("Size without Class Catalog : "+sizeWithoutCatalog);
        System.out.println("Size with Class Catalog    :  "+sizeWithCatalog);

        assertTrue(sizeWithoutCatalog>sizeWithCatalog);
    }

}
