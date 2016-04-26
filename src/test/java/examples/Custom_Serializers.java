package examples;

import org.junit.Test;
import org.mapdb.elsa.*;

import java.io.*;

/**
 * Demonstrates howto plug your own Serializer and Deserializer into Elsa
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
    SerializerBase.Deser<String2> deser = new SerializerBase.Deser<String2>() {
        @Override
        public String2 deserialize(DataInput in, SerializerBase.FastArrayList objectStack) throws IOException {
            return new String2(in.readUTF());
        }
    };


    /** serializer which converts String2 into binary form */
    SerializerBase.Ser<String2> ser = new SerializerBase.Ser<String2>() {
        @Override
        public void serialize(DataOutput out, String2 value, SerializerBase.FastArrayList objectStack) throws IOException {
            out.writeUTF(value.s);
        }
    };

    @Test public void serDeser() throws IOException {

        SerializerPojo s = new ElsaMaker()
                // register deserializer under STRING2_HEADER
                .registerDeser(STRING2_HEADER, deser)
                // register serializer with String2.class and STRING2_HEADEr
                .registerSer(STRING2_HEADER, String2.class, ser)
                .make();

        DataOutput out = new DataOutputStream(new ByteArrayOutputStream());
        s.serialize(out, new String2("some string"));
    }
}