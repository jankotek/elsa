package examples;

import org.junit.Test;
import org.mapdb.elsa.*;

import java.io.*;

/**
 * Demonstrates howto plug your own Serializer and Deserializer into Elsa.
 * It is part of Object Graph, so custom serializer is also used for values stored in field,
 * not just top-level objects.
 */
public class Custom_Serializers {

    /** header under which this class is serialized, must be unique for each class */
    static final int STRING2_HEADER = 22;

    /** class for which we implement serializer */
    static class String2{
        final String s;

        String2(String s) {
            this.s = s;
        }
    }


    /** deserializer which converts binary form into String2*/
    ElsaSerializerBase.Deserializer<String2> deser = new ElsaSerializerBase.Deserializer<String2>() {
        @Override
        public String2 deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            return new String2(in.readUTF());
        }
    };


    /** serializer which converts String2 into binary form */
    ElsaSerializerBase.Serializer<String2> ser = new ElsaSerializerBase.Serializer<String2>() {
        @Override
        public void serialize(DataOutput out, String2 value, ElsaStack objectStack) throws IOException {
            out.writeUTF(value.s);
        }
    };

    @Test public void serDeser() throws IOException {

        ElsaSerializerPojo s = new ElsaMaker()
                // register deserializer under STRING2_HEADER
                .registerDeserializer(STRING2_HEADER, deser)
                // register serializer with String2.class and STRING2_HEADEr
                .registerSerializer(STRING2_HEADER, String2.class, ser)
                .make();

        DataOutput out = new DataOutputStream(new ByteArrayOutputStream());
        s.serialize(out, new String2("some string"));
    }
}
