package org.mapdb.elsa;

import org.fest.reflect.core.Reflection;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElsaMakerTest {

    Class o;

    @Test
    public void unknownClass() throws IOException {
        ClassCallback callback = new ClassCallback() {
            @Override
            public void classMissing(Class clazz) {
                o = clazz;
            }
        };
        SerializerPojo s = new ElsaMaker().unknownClassNotification(callback).make();
        SerializerBaseTest.clonePojo(new Serialization2Bean(), s);
        assertEquals(Serialization2Bean.class, o);
    }

    static class String2{
        final String s;

        String2(String s) {
            this.s = s;
        }
  }

    int deserCounter = 0;

    SerializerBase.Deser<String2> deser = new SerializerBase.Deser<String2>() {
        @Override
        public String2 deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            deserCounter++;
            return new String2(in.readUTF());
        }
    };

    int serCounter = 0;

    SerializerBase.Ser<String2> ser = new SerializerBase.Ser<String2>() {
        @Override
        public void serialize(DataOutput out, String2 value, ElsaStack objectStack) throws IOException {
            serCounter++;
            out.writeUTF(value.s);
        }
    };

    @Test public void serDeser() throws IOException {

        SerializerPojo s = new ElsaMaker()
                .registerDeser(1, deser)
                .registerSer(1, String2.class, ser)
                .make();

        String2 str = new String2("adqwdwq");
        String2 str2 = SerializerBaseTest.clonePojo(str, s);
        assertEquals(str.s,str2.s);
        assertEquals(1, deserCounter);
        assertEquals(1, serCounter);
    }

    @Test public void objectStackNoRef(){
        SerializerPojo ser = new ElsaMaker().objectStackDisable().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.NoReferenceStack);
    }


    @Test public void objectStackHash(){
        SerializerPojo ser = new ElsaMaker().objectStackHashEnable().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.IdentityHashMapStack);
    }


    @Test public void objectStackDefault(){
        SerializerPojo ser = new ElsaMaker().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.IdentityArray);
    }
}
