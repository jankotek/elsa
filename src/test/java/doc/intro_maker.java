package doc;

import org.junit.Test;
import org.mapdb.elsa.ElsaMaker;
import org.mapdb.elsa.ElsaSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Shows configuration with Maker Patern
 */
public class intro_maker {

    @Test
    public void hello() throws Exception {
        //#a
        // import org.mapdb.elsa.*;

        // data to be serialized
        String data = "Hello World";

        // Construct Elsa Serializer
        // Elsa uses Maker Pattern to configure extra features
        ElsaSerializer serializer = new ElsaMaker().make();

        // Elsa Serializer takes DataOutput and DataInput.
        // Use streams to create it.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);

        // write data into OutputStream
        serializer.serialize(out2, data);

        // Construct DataInput
        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(out.toByteArray()));

        // now deserialize data using DataInput
        String data2 = serializer.deserialize(in);
        //#z
    }
}