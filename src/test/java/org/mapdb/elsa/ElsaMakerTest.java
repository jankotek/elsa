package org.mapdb.elsa;

import org.fest.reflect.core.Reflection;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElsaMakerTest {

    Class o;

    @Test
    public void unknownClass() throws IOException {
        ElsaClassCallback callback = new ElsaClassCallback() {
            @Override
            public void classMissing(Class clazz) {
                o = clazz;
            }
        };
        ElsaSerializerPojo s = new ElsaMaker().unknownClassNotification(callback).make();
        ElsaSerializerBaseTest.clonePojo(new Serialization2Bean(), s);
        assertEquals(Serialization2Bean.class, o);
    }

    static class String2{
        final String s;

        String2(String s) {
            this.s = s;
        }
  }

    int deserCounter = 0;

    ElsaSerializerBase.Deser<String2> deser = new ElsaSerializerBase.Deser<String2>() {
        @Override
        public String2 deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            deserCounter++;
            return new String2(in.readUTF());
        }
    };

    int serCounter = 0;

    ElsaSerializerBase.Ser<String2> ser = new ElsaSerializerBase.Ser<String2>() {
        @Override
        public void serialize(DataOutput out, String2 value, ElsaStack objectStack) throws IOException {
            serCounter++;
            out.writeUTF(value.s);
        }
    };

    @Test public void serDeser() throws IOException {

        ElsaSerializerPojo s = new ElsaMaker()
                .registerDeser(1, deser)
                .registerSer(1, String2.class, ser)
                .make();

        String2 str = new String2("adqwdwq");
        String2 str2 = ElsaSerializerBaseTest.clonePojo(str, s);
        assertEquals(str.s,str2.s);
        assertEquals(1, deserCounter);
        assertEquals(1, serCounter);
    }

    @Test public void objectStackNoRef(){
        ElsaSerializerPojo ser = new ElsaMaker().referenceDisable().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.NoReferenceStack);
    }


    @Test public void objectStackIdentHash(){
        ElsaSerializerPojo ser = new ElsaMaker().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.MapStack);
        assertTrue(((ElsaStack.MapStack)stack).data instanceof IdentityHashMap);
    }



    @Test public void objectStackHash(){
        ElsaSerializerPojo ser = new ElsaMaker().referenceHashMapEnable().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.MapStack);
        assertTrue(((ElsaStack.MapStack)stack).data instanceof HashMap);
    }

    @Test public void objectStackDefault(){
        ElsaSerializerPojo ser = new ElsaMaker().referenceArrayEnable().make();
        Object stack = Reflection.method("newElsaStack").withReturnType(ElsaStack.class).in(ser).invoke();
        assertTrue(stack instanceof ElsaStack.IdentityArray);
    }
}
