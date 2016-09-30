/*
 *  Copyright (c) 2012 Jan Kotek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mapdb.elsa;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializer which uses 'header byte' to serialize/deserialize
 * most of classes from 'java.lang' and 'java.util' packages.
 *
 * @author Jan Kotek
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ElsaSerializerBase implements ElsaSerializer{


    public static abstract class Ser<A> {
        /**
         * Serialize the content of an object into a ObjectOutput
         *
         * @param out ObjectOutput to save object into
         * @param value Object to serialize
         */
        abstract public void serialize( DataOutput out, A value, ElsaStack objectStack)
                throws IOException;
    }

    public static abstract class Deser<A> {

        /**
         * Deserialize the content of an object from a DataInput.
         *
         * @param in to read serialized data from
         * @return deserialized object
         * @throws java.io.IOException
         */
        abstract public A deserialize(DataInput in,  ElsaStack objectStack)
                throws IOException;

        public boolean needsObjectStack(){
            return false;
        }
    }

    /** always returns single object without reading anything*/
    protected final class DeserSingleton extends Deser{

        protected final Object singleton;

        public DeserSingleton(Object singleton) {
            this.singleton = singleton;
        }

        @Override
        public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            return singleton;
        }
    }

    protected final static class UserSer extends Ser{
        protected final int header;
        protected final Ser ser;

        public UserSer(int header, Ser ser) {
            this.header = header;
            this.ser = ser;
        }

        @Override
        public void serialize(DataOutput out, Object value, ElsaStack objectStack) throws IOException {
            out.write(Header.USER_DESER);
            ElsaUtil.packInt(out, header);
            ser.serialize(out,value, objectStack);
        }
    }

    protected static final class DeserStringLen extends Deser{
        final int len;

        DeserStringLen(int len) {
            this.len = len;
        }

        @Override
        public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            return deserializeString(in,len);
        }
    }


    protected static final class DeserInt extends Deser{

        protected final int digits;
        protected final boolean minus;

        public DeserInt(int digits, boolean minus) {
            this.digits = digits;
            this.minus = minus;
        }

        @Override
        public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            int ret = in.readUnsignedByte()&0xFF;
            for(int i=1;i<digits;i++){
                ret = (ret<<8) | (in.readUnsignedByte()&0xFF);
            }
            if(minus)
                ret = -ret;
            return ret;
        }
    }

    protected static final class DeserLong extends Deser{

        protected final int digits;
        protected final boolean minus;

        public DeserLong(int digits, boolean minus) {
            this.digits = digits;
            this.minus = minus;
        }

        @Override
        public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            long ret = in.readUnsignedByte()&0xFF;
            for(int i=1;i<digits;i++){
                ret = (ret<<8) | (in.readUnsignedByte()&0xFF);
            }
            if(minus)
                ret = -ret;
            return ret;
        }
    }

    protected final int objectStackType;
    protected final Object[] singletons;
    protected final IdentityHashMap<Object, Integer> singletonsReverse = new IdentityHashMap();


    protected final Map<Class, Ser> ser = new IdentityHashMap<Class, Ser>();
    protected final Map<String, Class> classCache = new ConcurrentHashMap<String, Class>();

    protected final Deser[] headerDeser = new Deser[255];
    protected final Deser[] userDeser;

    //TODO configurable class loader?
    protected final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    protected Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, classLoader);
    }

        protected Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        Class c = classCache.get(name);
        if(c==null) {
            //load class and put it into cache
            //this is thread safe, worst case is that `Class.forName` will be called more than once at initialization, which is fine
            c = Class.forName(name, true, classLoader);
            classCache.put(name, c);
        }
        return c;
    }


    protected Class loadClass2(DataInput is) throws IOException {
        return loadClass2(is.readUTF());
    }


    protected Class loadClass2(String name){
        try {
            return loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e); //TODO exception hierarchy
        }
    }


    static protected Class loadClass3(String name, ClassLoader classLoader){
        try {
            return Class.forName(name, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e); //TODO exception hierarchy
        }
    }
    public ElsaSerializerBase(){
        this(0, null, null, null, null);
    }

    public ElsaSerializerBase(
            int objectStackType,
            Object[] singletons,
            Map<Class, Ser> userSer,
            Map<Class, Integer> userSerHeaders,
            Map<Integer, Deser> userDeser){
        this.objectStackType = objectStackType;
        this.singletons = singletons!=null? singletons.clone():new Object[0];
        for(int i=0;i<this.singletons.length;i++){
            singletonsReverse.put(this.singletons[i], i);
        }
        initHeaderDeser();
        initSer();

        if(!(userSer==null && userSerHeaders==null && userDeser==null)){
            //add user defined serializers
            if(!userSer.keySet().equals(userSerHeaders.keySet()))
                throw new IllegalArgumentException();
            if(new TreeSet(userSerHeaders.values()).size()!=userDeser.size())
                throw new IllegalArgumentException();
            if(!userDeser.keySet().equals(new TreeSet(userSerHeaders.values())))
                throw new IllegalArgumentException();

            //register serializers for each class
            for(Class clazz:userSer.keySet()){
                int userHeader = userSerHeaders.get(clazz);
                ser.put(clazz, new UserSer(userHeader, userSer.get(clazz)));
            }

            //register deserialization for each user header
            int maxHeader = userDeser.size()==0?0:new TreeSet<Integer>(userDeser.keySet()).last();
            this.userDeser = new Deser[maxHeader+1];
            for(Integer header:userDeser.keySet())
                this.userDeser[header] = userDeser.get(header);
        }else{
            this.userDeser = new Deser[0];
        }
    }


    protected void initSer() {
        ser.put(Integer.class, SER_INT);
        ser.put(Long.class, SER_LONG);
        ser.put(String.class, SER_STRING);
        ser.put(Boolean.class, SER_BOOLEAN);
        ser.put(String.class, SER_STRING);
        ser.put(Character.class, SER_CHAR);
        ser.put(Short.class, SER_SHORT);
        ser.put(Float.class, SER_FLOAT);
        ser.put(Double.class, SER_DOUBLE);
        ser.put(Byte.class, SER_BYTE);

        ser.put(byte[].class, SER_BYTE_ARRAY);
        ser.put(boolean[].class, new Ser<boolean[]>() {
            @Override
            public void serialize(DataOutput out, boolean[] value, ElsaStack objectStack) throws IOException {
                out.write(Header.ARRAY_BOOLEAN);
                ElsaUtil.packInt(out, value.length);//write the number of booleans not the number of bytes
                ElsaSerializerBase.writeBooleanArray(out,value);
            }
        });
        ser.put(char[].class, new Ser<char[]>() {
            @Override
            public void serialize(DataOutput out, char[] value, ElsaStack objectStack) throws IOException {
                out.write(Header.ARRAY_CHAR);
                ElsaUtil.packInt(out,value.length);
                for(char v:value){
                    ElsaUtil.packInt(out,v);
                }
            }
        });
        ser.put(short[].class, new Ser<short[]>() {
            @Override
            public void serialize(DataOutput out, short[] value, ElsaStack objectStack) throws IOException {
                out.write(Header.ARRAY_SHORT);
                ElsaUtil.packInt(out,value.length);
                for(short v:value){
                    out.writeShort(v);
                }
            }
        });
        ser.put(float[].class, new Ser<float[]>() {
            @Override
            public void serialize(DataOutput out, float[] value, ElsaStack objectStack) throws IOException {
                out.write(Header.ARRAY_FLOAT);
                ElsaUtil.packInt(out,value.length);
                for(float v:value){
                    out.writeFloat(v);
                }
            }
        });
        ser.put(double[].class, new Ser<double[]>() {
            @Override
            public void serialize(DataOutput out, double[] value, ElsaStack objectStack) throws IOException {
                out.write(Header.ARRAY_DOUBLE);
                ElsaUtil.packInt(out,value.length);
                for(double v:value){
                    out.writeDouble(v);
                }
            }
        });
        ser.put(int[].class, SER_INT_ARRAY);
        ser.put(long[].class, SER_LONG_ARRAY);

        ser.put(BigInteger.class, new Ser<BigInteger>() {
            @Override
            public void serialize(DataOutput out, BigInteger value, ElsaStack objectStack) throws IOException {
                out.write(Header.BIGINTEGER);
                byte[] b = value.toByteArray();
                ElsaUtil.packInt(out, b.length);
                out.write(b);
            }
        });

        ser.put(BigDecimal.class, new Ser<BigDecimal>() {
            @Override
            public void serialize(DataOutput out, BigDecimal value, ElsaStack objectStack) throws IOException {
                out.write(Header.BIGDECIMAL);
                byte[] b = value.unscaledValue().toByteArray();
                ElsaUtil.packInt(out, b.length);
                out.write(b);
                ElsaUtil.packInt(out, value.scale());
            }
        });

        ser.put(Class.class, new Ser<Class<?>>(){
            @Override
            public void serialize(DataOutput out, Class<?> value, ElsaStack objectStack) throws IOException {
                out.write(Header.CLASS);
                out.writeUTF(value.getName());
            }
        });

        ser.put(Date.class, new Ser<Date>(){
            @Override
            public void serialize(DataOutput out, Date value, ElsaStack objectStack) throws IOException {
                out.write(Header.DATE);
                out.writeLong(value.getTime());
            }
        });

        ser.put(UUID.class, new Ser<UUID>(){
            @Override
            public void serialize(DataOutput out, UUID value, ElsaStack objectStack) throws IOException {
                out.write(Header.UUID);
                out.writeLong(value.getMostSignificantBits());
                out.writeLong(value.getLeastSignificantBits());
            }
        });

        ser.put(Object[].class, new Ser<Object[]>(){

            @Override
            public void serialize(DataOutput out, Object[] b, ElsaStack objectStack) throws IOException {
                serializeObjectArray(out, b, objectStack);
            }
        });

        ser.put(ArrayList.class, new Ser<ArrayList>(){
            @Override
            public void serialize(DataOutput out, ArrayList value, ElsaStack objectStack) throws IOException {
                serializeCollection(Header.ARRAYLIST, out, value, objectStack);
            }
        });

        ser.put(LinkedList.class, new Ser<Collection>(){
            @Override
            public void serialize(DataOutput out, Collection value, ElsaStack objectStack) throws IOException {
                serializeCollection(Header.LINKEDLIST, out,value, objectStack);
            }
        });

        ser.put(HashSet.class, new Ser<Collection>(){
            @Override
            public void serialize(DataOutput out, Collection value, ElsaStack objectStack) throws IOException {
                serializeCollection(Header.HASHSET, out,value, objectStack);
            }
        });

        ser.put(LinkedHashSet.class, new Ser<Collection>(){
            @Override
            public void serialize(DataOutput out, Collection value, ElsaStack objectStack) throws IOException {
                serializeCollection(Header.LINKEDHASHSET, out,value, objectStack);
            }
        });

        ser.put(HashMap.class, new Ser<Map>(){
            @Override
            public void serialize(DataOutput out, Map value, ElsaStack objectStack) throws IOException {
                serializeMap(Header.HASHMAP, out,value, objectStack);
            }
        });

        ser.put(LinkedHashMap.class, new Ser<Map>(){
            @Override
            public void serialize(DataOutput out, Map value, ElsaStack objectStack) throws IOException {
                serializeMap(Header.LINKEDHASHMAP, out,value, objectStack);
            }
        });

        ser.put(Properties.class, new Ser<Map>(){
            @Override
            public void serialize(DataOutput out, Map value, ElsaStack objectStack) throws IOException {
                serializeMap(Header.PROPERTIES, out, value, objectStack);
            }
        });


        ser.put(TreeSet.class, new Ser<TreeSet>(){
            @Override
            public void serialize(DataOutput out, TreeSet l, ElsaStack objectStack) throws IOException {
                out.write(Header.TREESET);
                ElsaUtil.packInt(out, l.size());
                ElsaSerializerBase.this.serialize(out, l.comparator(), objectStack);
                for (Object o : l)
                    ElsaSerializerBase.this.serialize(out, o, objectStack);
            }
        });

        ser.put(TreeMap.class, new Ser<TreeMap<Object,Object>>(){
            @Override
            public void serialize(DataOutput out, TreeMap<Object,Object> l, ElsaStack objectStack) throws IOException {
                out.write(Header.TREEMAP);
                ElsaUtil.packInt(out, l.size());
                ElsaSerializerBase.this.serialize(out, l.comparator(), objectStack);
                for (Map.Entry o : l.entrySet()) {
                    ElsaSerializerBase.this.serialize(out, o.getKey(), objectStack);
                    ElsaSerializerBase.this.serialize(out, o.getValue(), objectStack);
                }
            }
        });
        //TODO object stack handling is probably all broken. write paranoid tests!!!
        //TODO write automated test to check if static classes inside BTreeKeySer.. .and other can be serialized
    }

    public void serializeObjectArray(DataOutput out, Object[] b, ElsaStack objectStack) throws IOException {
        boolean allNull = true;
        //check for all null
        for (Object o : b) {
         if(o!=null){
            allNull=false;
            break;
         }
        }

        if(allNull){
            out.write(Header.ARRAY_OBJECT_ALL_NULL);
            ElsaUtil.packInt(out, b.length);

            // Write class for components
            Class<?> componentType = b.getClass().getComponentType();
            serializeClass(out, componentType);
        } else {
            out.write(Header.ARRAY_OBJECT);
            ElsaUtil.packInt(out, b.length);

            // Write class for components
            Class<?> componentType = b.getClass().getComponentType();
            serializeClass(out, componentType);

            for (Object o : b)
                this.serialize(out, o, objectStack);

        }
    }

    protected void initHeaderDeser(){

        headerDeser[Header.NULL] = new DeserSingleton(null);
        headerDeser[Header.ZERO_FAIL] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                throw new IOError(new IOException("Zero Header, data corrupted"));
            }
        };
        headerDeser[Header.JAVA_SERIALIZATION] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                throw new IOError(new IOException(
                        "Wrong header, data were probably serialized with " +
                                "java.lang.ObjectOutputStream," +
                                " not with MapDB serialization"));
            }
        };

        headerDeser[Header.BOOLEAN_FALSE] = new DeserSingleton(Boolean.FALSE);
        headerDeser[Header.BOOLEAN_TRUE] = new DeserSingleton(Boolean.TRUE);

        headerDeser[Header.INT_M9] = new DeserSingleton(-9);
        headerDeser[Header.INT_M8] = new DeserSingleton(-8);
        headerDeser[Header.INT_M7] = new DeserSingleton(-7);
        headerDeser[Header.INT_M6] = new DeserSingleton(-6);
        headerDeser[Header.INT_M5] = new DeserSingleton(-5);
        headerDeser[Header.INT_M4] = new DeserSingleton(-4);
        headerDeser[Header.INT_M3] = new DeserSingleton(-3);
        headerDeser[Header.INT_M2] = new DeserSingleton(-2);
        headerDeser[Header.INT_M1] = new DeserSingleton(-1);
        headerDeser[Header.INT_0] = new DeserSingleton(0);
        headerDeser[Header.INT_1] = new DeserSingleton(1);
        headerDeser[Header.INT_2] = new DeserSingleton(2);
        headerDeser[Header.INT_3] = new DeserSingleton(3);
        headerDeser[Header.INT_4] = new DeserSingleton(4);
        headerDeser[Header.INT_5] = new DeserSingleton(5);
        headerDeser[Header.INT_6] = new DeserSingleton(6);
        headerDeser[Header.INT_7] = new DeserSingleton(7);
        headerDeser[Header.INT_8] = new DeserSingleton(8);
        headerDeser[Header.INT_9] = new DeserSingleton(9);
        headerDeser[Header.INT_10] = new DeserSingleton(10);
        headerDeser[Header.INT_11] = new DeserSingleton(11);
        headerDeser[Header.INT_12] = new DeserSingleton(12);
        headerDeser[Header.INT_13] = new DeserSingleton(13);
        headerDeser[Header.INT_14] = new DeserSingleton(14);
        headerDeser[Header.INT_15] = new DeserSingleton(15);
        headerDeser[Header.INT_16] = new DeserSingleton(16);
        headerDeser[Header.INT_MIN_VALUE] = new DeserSingleton(Integer.MIN_VALUE);
        headerDeser[Header.INT_MAX_VALUE] = new DeserSingleton(Integer.MAX_VALUE);

        headerDeser[Header.LONG_M9] = new DeserSingleton(-9L);
        headerDeser[Header.LONG_M8] = new DeserSingleton(-8L);
        headerDeser[Header.LONG_M7] = new DeserSingleton(-7L);
        headerDeser[Header.LONG_M6] = new DeserSingleton(-6L);
        headerDeser[Header.LONG_M5] = new DeserSingleton(-5L);
        headerDeser[Header.LONG_M4] = new DeserSingleton(-4L);
        headerDeser[Header.LONG_M3] = new DeserSingleton(-3L);
        headerDeser[Header.LONG_M2] = new DeserSingleton(-2L);
        headerDeser[Header.LONG_M1] = new DeserSingleton(-1L);
        headerDeser[Header.LONG_0] = new DeserSingleton(0L);
        headerDeser[Header.LONG_1] = new DeserSingleton(1L);
        headerDeser[Header.LONG_2] = new DeserSingleton(2L);
        headerDeser[Header.LONG_3] = new DeserSingleton(3L);
        headerDeser[Header.LONG_4] = new DeserSingleton(4L);
        headerDeser[Header.LONG_5] = new DeserSingleton(5L);
        headerDeser[Header.LONG_6] = new DeserSingleton(6L);
        headerDeser[Header.LONG_7] = new DeserSingleton(7L);
        headerDeser[Header.LONG_8] = new DeserSingleton(8L);
        headerDeser[Header.LONG_9] = new DeserSingleton(9L);
        headerDeser[Header.LONG_10] = new DeserSingleton(10L);
        headerDeser[Header.LONG_11] = new DeserSingleton(11L);
        headerDeser[Header.LONG_12] = new DeserSingleton(12L);
        headerDeser[Header.LONG_13] = new DeserSingleton(13L);
        headerDeser[Header.LONG_14] = new DeserSingleton(14L);
        headerDeser[Header.LONG_15] = new DeserSingleton(15L);
        headerDeser[Header.LONG_16] = new DeserSingleton(16L);
        headerDeser[Header.LONG_MIN_VALUE] = new DeserSingleton(Long.MIN_VALUE);
        headerDeser[Header.LONG_MAX_VALUE] = new DeserSingleton(Long.MAX_VALUE);

        headerDeser[Header.CHAR_0] = new DeserSingleton((char)0);
        headerDeser[Header.CHAR_1] = new DeserSingleton((char)1);

        headerDeser[Header.SHORT_M1] = new DeserSingleton((short)-1);
        headerDeser[Header.SHORT_0] = new DeserSingleton((short)0);
        headerDeser[Header.SHORT_1] = new DeserSingleton((short)1);

        headerDeser[Header.FLOAT_M1] = new DeserSingleton(-1F);
        headerDeser[Header.FLOAT_0] = new DeserSingleton(0F);
        headerDeser[Header.FLOAT_1] = new DeserSingleton(1F);

        headerDeser[Header.DOUBLE_M1] = new DeserSingleton(-1D);
        headerDeser[Header.DOUBLE_0] = new DeserSingleton(0D);
        headerDeser[Header.DOUBLE_1] = new DeserSingleton(1D);

        headerDeser[Header.BYTE_M1] = new DeserSingleton((byte)-1);
        headerDeser[Header.BYTE_0] = new DeserSingleton((byte)0);
        headerDeser[Header.BYTE_1] = new DeserSingleton((byte)1);

        headerDeser[Header.STRING_0] = new DeserSingleton("");

        headerDeser[Header.INT] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readInt();
            }
        };
        headerDeser[Header.LONG] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readLong();
            }
        };
        headerDeser[Header.CHAR] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readChar();
            }
        };
        headerDeser[Header.SHORT] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readShort();
            }
        };
        headerDeser[Header.FLOAT] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readFloat();
            }
        };
        headerDeser[Header.DOUBLE] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readDouble();
            }
        };
        headerDeser[Header.BYTE] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return in.readByte();
            }
        };

        headerDeser[Header.STRING] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeString(in, ElsaUtil.unpackInt(in));
            }
        };
        headerDeser[Header.STRING_1] = new DeserStringLen(1);
        headerDeser[Header.STRING_2] = new DeserStringLen(2);
        headerDeser[Header.STRING_3] = new DeserStringLen(3);
        headerDeser[Header.STRING_4] = new DeserStringLen(4);
        headerDeser[Header.STRING_5] = new DeserStringLen(5);
        headerDeser[Header.STRING_6] = new DeserStringLen(6);
        headerDeser[Header.STRING_7] = new DeserStringLen(7);
        headerDeser[Header.STRING_8] = new DeserStringLen(8);
        headerDeser[Header.STRING_9] = new DeserStringLen(9);
        headerDeser[Header.STRING_10] = new DeserStringLen(10);

        headerDeser[Header.CHAR_255] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (char) in.readUnsignedByte();
            }
        };

        headerDeser[Header.SHORT_255] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (short) in.readUnsignedByte();
            }
        };

        headerDeser[Header.SHORT_M255] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (short) -in.readUnsignedByte();
            }
        };

        headerDeser[Header.FLOAT_255] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (float) in.readUnsignedByte();
            }
        };

        headerDeser[Header.FLOAT_SHORT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (float) in.readShort();
            }
        };

        headerDeser[Header.DOUBLE_255] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (double) in.readUnsignedByte();
            }
        };

        headerDeser[Header.DOUBLE_SHORT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (double) in.readShort();
            }
        };

        headerDeser[Header.DOUBLE_INT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return (double) in.readInt();
            }
        };

        headerDeser[Header.ARRAY_BYTE_ALL_EQUAL] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                byte[] b = new byte[ElsaUtil.unpackInt(in)];
                Arrays.fill(b, in.readByte());
                return b;
            }
        };

        headerDeser[Header.ARRAY_BOOLEAN] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                return ElsaSerializerBase.readBooleanArray(size, in);
            }
        };
        headerDeser[Header.ARRAY_INT] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                int[] ret = new int[size];
                for(int i=0;i<size;i++){
                    ret[i] = in.readInt();
                }
                return ret;
            }
        };

        headerDeser[Header.ARRAY_LONG] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                long[] ret = new long[size];
                for(int i=0;i<size;i++){
                    ret[i] = in.readLong();
                }
                return ret;
            }
        };
        headerDeser[Header.ARRAY_SHORT] =  new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                short[] ret = new short[size];
                for(int i=0;i<size;i++){
                    ret[i] = in.readShort();
                }
                return ret;
            }
        };
        headerDeser[Header.ARRAY_DOUBLE] =  new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                double[] ret = new double[size];
                for(int i=0;i<size;i++){
                    ret[i] = in.readDouble();
                }
                return ret;
            }
        };
        headerDeser[Header.ARRAY_FLOAT]=  new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                float[] ret = new float[size];
                for(int i=0;i<size;i++){
                    ret[i] = in.readFloat();
                }
                return ret;
            }
        };
        headerDeser[Header.ARRAY_CHAR]= new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                char[] ret = new char[size];
                for(int i=0;i<size;i++){
                    ret[i] = (char)ElsaUtil.unpackInt(in);
                }
                return ret;
            }
        };
        headerDeser[Header.ARRAY_BYTE]= DESER_BYTE_ARRAY;

        headerDeser[Header.ARRAY_INT_BYTE] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int[] ret=new int[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = in.readByte();
                return ret;
            }
        };

        headerDeser[Header.ARRAY_INT_SHORT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int[] ret=new int[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = in.readShort();
                return ret;
            }
        };


        headerDeser[Header.ARRAY_INT_PACKED] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int[] ret=new int[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = ElsaUtil.unpackInt(in);
                return ret;
            }
        };


        headerDeser[Header.ARRAY_LONG_BYTE] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                long[] ret=new long[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = in.readByte();
                return ret;
            }
        };

        headerDeser[Header.ARRAY_LONG_SHORT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                long[] ret=new long[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = in.readShort();
                return ret;
            }
        };

        headerDeser[Header.ARRAY_LONG_INT] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                long[] ret=new long[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = in.readInt();
                return ret;
            }
        };

        headerDeser[Header.ARRAY_LONG_PACKED] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                long[] ret=new long[ElsaUtil.unpackInt(in)];
                for(int i=0;i<ret.length;i++)
                    ret[i] = ElsaUtil.unpackLong(in);
                return ret;
            }
        };

        headerDeser[Header.BIGINTEGER] = new Deser(){
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return new BigInteger(DESER_BYTE_ARRAY.deserialize(in,objectStack));
            }
        };
        headerDeser[Header.BIGDECIMAL] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return new BigDecimal(new BigInteger(
                        DESER_BYTE_ARRAY.deserialize(in,objectStack)),
                        ElsaUtil.unpackInt(in));
            }
        };
        headerDeser[Header.CLASS] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                try {
                    return loadClass(in.readUTF());
                } catch (ClassNotFoundException e) {
                    throw new IOException(e);
                }
            }
        };
        headerDeser[Header.DATE] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return new Date(in.readLong());
            }
        };
        headerDeser[Header.UUID] = new Deser() {
            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return new UUID(in.readLong(), in.readLong());
            }
        };

        headerDeser[Header.ARRAY_OBJECT_ALL_NULL] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int size = ElsaUtil.unpackInt(in);
                Class clazz = loadClass2(in);
                return java.lang.reflect.Array.newInstance(clazz, size);
            }
        };
        headerDeser[Header.ARRAY_OBJECT_NO_REFS] = new Deser(){
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                //TODO serializatio code for this does not exist, add it in future
                int size = ElsaUtil.unpackInt(in);
                Class clazz = loadClass2(in);
                Object[] s = (Object[]) java.lang.reflect.Array.newInstance(clazz, size);
                for (int i = 0; i < size; i++){
                    s[i] = ElsaSerializerBase.this.deserialize(in, null);
                }
                return s;
            }
        };

        headerDeser[Header.OBJECT_STACK] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return objectStack.getInstance(ElsaUtil.unpackInt(in));
            }

            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.ARRAYLIST] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeArrayList(in, objectStack);
            }

            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.ARRAY_OBJECT] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeArrayObject(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.LINKEDLIST] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeLinkedList(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.TREESET] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeTreeSet(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.HASHSET] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeHashSet(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.LINKEDHASHSET] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeLinkedHashSet(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.TREEMAP] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeTreeMap(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.HASHMAP] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeHashMap(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.LINKEDHASHMAP] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeLinkedHashMap(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };

        headerDeser[Header.PROPERTIES] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeProperties(in, objectStack);
            }
            @Override public boolean needsObjectStack() {
                return true;
            }
        };


        headerDeser[Header.SINGLETON] = new Deser() {
            @Override public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                return deserializeSingleton(in,objectStack);
            }
            @Override public boolean needsObjectStack() {
                return false;
            }
        };

        headerDeser[Header.USER_DESER] = new Deser(){

            @Override
            public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {
                int userHeader = ElsaUtil.unpackInt(in);
                if(userHeader>userDeser.length || userDeser[userHeader]==null)
                    throw new ElsaException("No user deserializer defined for user header "+userHeader);
                return userDeser[userHeader].deserialize(in, objectStack);
            }

        };


        headerDeser[Header.INT_MF3] = new DeserInt(3,true);
        headerDeser[ Header.INT_F3] = new DeserInt(3,false);
        headerDeser[Header.INT_MF2] = new DeserInt(2,true);
        headerDeser[ Header.INT_F2] = new DeserInt(2,false);
        headerDeser[Header.INT_MF1] = new DeserInt(1,true);
        headerDeser[ Header.INT_F1] = new DeserInt(1,false);

        headerDeser[Header.LONG_MF7] = new DeserLong(7,true);
        headerDeser[ Header.LONG_F7] = new DeserLong(7,false);
        headerDeser[Header.LONG_MF6] = new DeserLong(6,true);
        headerDeser[ Header.LONG_F6] = new DeserLong(6,false);
        headerDeser[Header.LONG_MF5] = new DeserLong(5,true);
        headerDeser[ Header.LONG_F5] = new DeserLong(5,false);
        headerDeser[Header.LONG_MF4] = new DeserLong(4,true);
        headerDeser[ Header.LONG_F4] = new DeserLong(4,false);
        headerDeser[Header.LONG_MF3] = new DeserLong(3,true);
        headerDeser[ Header.LONG_F3] = new DeserLong(3,false);
        headerDeser[Header.LONG_MF2] = new DeserLong(2,true);
        headerDeser[ Header.LONG_F2] = new DeserLong(2,false);
        headerDeser[Header.LONG_MF1] = new DeserLong(1,true);
        headerDeser[ Header.LONG_F1] = new DeserLong(1,false);

    }


    @Override
    public void serialize(final DataOutput out, final Object obj) throws IOException {
        serialize(out, obj, newElsaStack());
    }

    protected ElsaStack newElsaStack() {
        switch(objectStackType){
            case 3: return new ElsaStack.MapStack(new HashMap());
            case 2: return new ElsaStack.IdentityArray();
            case 1: return new ElsaStack.NoReferenceStack();
            case 0: return new ElsaStack.MapStack(new IdentityHashMap());
            default: throw new IllegalArgumentException("Unknown objectStackType:  " +objectStackType);
        }
    }

    @Override
    public <E> E clone(E value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream out2 = new DataOutputStream(out);
        serialize(out2, value);

        DataInputStream ins = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        return (E) deserialize(ins);
    }

    public void serialize(final DataOutput out, final Object obj, ElsaStack objectStack) throws IOException {

        if (obj == null) {
            out.write(Header.NULL);
            return;
        }

        /**try to find object on stack if it exists*/
        if (objectStack != null) {
            int indexInObjectStack = objectStack.identityIndexOf(obj);
            if (indexInObjectStack != -1) {
                //object was already serialized, just write reference to it and return
                out.write(Header.OBJECT_STACK);
                ElsaUtil.packInt(out, indexInObjectStack);
                return;
            }
            //add this object to objectStack
            objectStack.add(obj);
        }


        //Object[] and String[] are two different classes,
        // so getClass()==getClass() fails, but instanceof works
        // so special treatment for non-primitive arrays
        if(obj instanceof Object[]){
            serializeObjectArray(out, (Object[]) obj, objectStack);
            return;
        }

        //try mapdb singletons
        final Integer mapdbSingletonHeader = singletonsReverse.get(obj);
        if(mapdbSingletonHeader!=null){
            out.write(Header.SINGLETON);
            ElsaUtil.packInt(out, mapdbSingletonHeader);
            return;
        }

        Ser s = ser.get(obj.getClass());
        if(s!=null){
            s.serialize(out,obj,objectStack);
            return;
        }

        //unknown clas
        serializeUnknownObject(out,obj,objectStack);
    }


    protected static final Ser SER_STRING = new Ser<String>(){
        @Override
        public void serialize(DataOutput out, String value, ElsaStack objectStack) throws IOException {
            int len = value.length();
            if(len == 0){
                out.write(Header.STRING_0);
            }else{
                if (len<=10){
                    out.write(Header.STRING_0+len);
                }else{
                    out.write(Header.STRING);
                    ElsaUtil.packInt(out, len);
                }
                for (int i = 0; i < len; i++)
                    ElsaUtil.packInt(out,(int)(value.charAt(i)));
            }
        }
    };

    protected static final Ser SER_LONG_ARRAY = new Ser<long[]>() {
        @Override
        public void serialize(DataOutput out, long[] val, ElsaStack objectStack) throws IOException {

            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;
            for (long i : val) {
                max = Math.max(max, i);
                min = Math.min(min, i);
            }
            if (Byte.MIN_VALUE <= min && max <= Byte.MAX_VALUE) {
                out.write(Header.ARRAY_LONG_BYTE);
                ElsaUtil.packInt(out, val.length);
                for (long i : val) out.write((int) i);
            } else if (Short.MIN_VALUE <= min && max <= Short.MAX_VALUE) {
                out.write(Header.ARRAY_LONG_SHORT);
                ElsaUtil.packInt(out, val.length);
                for (long i : val) out.writeShort((int) i);
            } else if (0 <= min) {
                out.write(Header.ARRAY_LONG_PACKED);
                ElsaUtil.packInt(out, val.length);
                for (long l : val) ElsaUtil.packLong(out, l);
            } else if (Integer.MIN_VALUE <= min && max <= Integer.MAX_VALUE) {
                out.write(Header.ARRAY_LONG_INT);
                ElsaUtil.packInt(out, val.length);
                for (long i : val) out.writeInt((int) i);
            } else {
                out.write(Header.ARRAY_LONG);
                ElsaUtil.packInt(out, val.length);
                for (long i : val) out.writeLong(i);
            }
        }
    };

    protected static final Ser SER_INT_ARRAY = new Ser<int[]>() {
        @Override
        public void serialize(DataOutput out, int[] val, ElsaStack objectStack) throws IOException {

            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (int i : val) {
                max = Math.max(max, i);
                min = Math.min(min, i);
            }
            if (Byte.MIN_VALUE <= min && max <= Byte.MAX_VALUE) {
                out.write(Header.ARRAY_INT_BYTE);
                ElsaUtil.packInt(out, val.length);
                for (int i : val) out.write(i);
            } else if (Short.MIN_VALUE <= min && max <= Short.MAX_VALUE) {
                out.write(Header.ARRAY_INT_SHORT);
                ElsaUtil.packInt(out, val.length);
                for (int i : val) out.writeShort(i);
            } else if (0 <= min) {
                out.write(Header.ARRAY_INT_PACKED);
                ElsaUtil.packInt(out, val.length);
                for (int l : val) ElsaUtil.packInt(out, l);
            } else {
                out.write(Header.ARRAY_INT);
                ElsaUtil.packInt(out, val.length);
                for (int i : val) out.writeInt(i);
            }
        }
    };

    protected static final Ser SER_DOUBLE = new Ser<Double>() {
        @Override
        public void serialize(DataOutput out, Double value, ElsaStack objectStack) throws IOException {
            double v = value;
            if (v == -1D) {
                out.write(Header.DOUBLE_M1);
            } else if (v == 0D) {
                out.write(Header.DOUBLE_0);
            } else if (v == 1D) {
                out.write(Header.DOUBLE_1);
            } else if (v >= 0 && v <= 255 && value.intValue() == v) {
                out.write(Header.DOUBLE_255);
                out.write(value.intValue());
            } else if (value.shortValue() == v) {
                out.write(Header.DOUBLE_SHORT);
                out.writeShort(value.shortValue());
            } else if (value.intValue() == v) {
                out.write(Header.DOUBLE_INT);
                out.writeInt(value.intValue());
            } else {
                out.write(Header.DOUBLE);
                out.writeDouble(v);
            }
        }
    };

    protected static final Ser SER_FLOAT = new Ser<Float>() {
        @Override
        public void serialize(DataOutput out, Float value, ElsaStack objectStack) throws IOException {
            float v = value;
            if (v == -1f)
                out.write(Header.FLOAT_M1);
            else if (v == 0f)
                out.write(Header.FLOAT_0);
            else if (v == 1f)
                out.write(Header.FLOAT_1);
            else if (v >= 0 && v <= 255 && value.intValue() == v) {
                out.write(Header.FLOAT_255);
                out.write(value.intValue());
            } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE && value.shortValue() == v) {
                out.write(Header.FLOAT_SHORT);
                out.writeShort(value.shortValue());
            } else {
                out.write(Header.FLOAT);
                out.writeFloat(v);
            }
        }
    };

    protected static final Ser SER_SHORT = new Ser<Short>() {
        @Override
        public void serialize(DataOutput out, Short value, ElsaStack objectStack) throws IOException {

            short val = value;
            if (val == -1) {
                out.write(Header.SHORT_M1);
            } else if (val == 0) {
                out.write(Header.SHORT_0);
            } else if (val == 1) {
                out.write(Header.SHORT_1);
            } else if (val > 0 && val < 255) {
                out.write(Header.SHORT_255);
                out.write(val);
            } else if (val < 0 && val > -255) {
                out.write(Header.SHORT_M255);
                out.write(-val);
            } else {
                out.write(Header.SHORT);
                out.writeShort(val);
            }
        }
    };

    protected static final Ser SER_CHAR = new Ser<Character>() {
        @Override
        public void serialize(DataOutput out, Character value, ElsaStack objectStack) throws IOException {
            char val = value;
            if (val == 0) {
                out.write(Header.CHAR_0);
            } else if (val == 1) {
                out.write(Header.CHAR_1);
            } else if (val <= 255) {
                out.write(Header.CHAR_255);
                out.write(val);
            } else {
                out.write(Header.CHAR);
                out.writeChar(val);
            }
        }
    };

    protected static final Ser SER_BYTE= new Ser<Byte>() {
        @Override
        public void serialize(DataOutput out, Byte value, ElsaStack objectStack) throws IOException {
            byte val = value;
            if (val == -1)
                out.write(Header.BYTE_M1);
            else if (val == 0)
                out.write(Header.BYTE_0);
            else if (val == 1)
                out.write(Header.BYTE_1);
            else {
                out.write(Header.BYTE);
                out.writeByte(val);
            }
        }
    };

    protected static final Ser SER_BOOLEAN = new Ser<Boolean>() {
        @Override
        public void serialize(DataOutput out, Boolean value, ElsaStack objectStack) throws IOException {
            out.write(value ? Header.BOOLEAN_TRUE : Header.BOOLEAN_FALSE);
        }
    };


    protected static final Ser SER_LONG = new Ser<Long>() {
        @Override
        public void serialize(DataOutput out, Long value, ElsaStack objectStack) throws IOException {
            long val = value;
            if (val >= -9 && val <= 16) {
                out.write((int) (Header.LONG_M9 + (val + 9)));
                return;
            } else if (val == Long.MIN_VALUE) {
                out.write(Header.LONG_MIN_VALUE);
                return;
            } else if (val == Long.MAX_VALUE) {
                out.write(Header.LONG_MAX_VALUE);
                return;
            } else if (((Math.abs(val) >>> 56) & 0xFF) != 0) {
                out.write(Header.LONG);
                out.writeLong(val);
                return;
            }

            int neg = 0;
            if (val < 0) {
                neg = -1;
                val = -val;
            }

            //calculate N bytes
            int size = 48;
            while (((val >> size) & 0xFFL) == 0) {
                size -= 8;
            }

            //write header
            out.write(Header.LONG_F1 + (size / 8) * 2 + neg);

            //write data
            while (size >= 0) {
                out.write((int) ((val >> size) & 0xFFL));
                size -= 8;
            }
        }
    };

    protected static final Ser SER_INT = new Ser<Integer>() {
        @Override
        public void serialize(DataOutput out, Integer value, ElsaStack objectStack) throws IOException {
            int val = value;
            switch (val) {
                case -9:
                case -8:
                case -7:
                case -6:
                case -5:
                case -4:
                case -3:
                case -2:
                case -1:
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    out.write((Header.INT_M9 + (val + 9)));
                    return;
                case Integer.MIN_VALUE:
                    out.write(Header.INT_MIN_VALUE);
                    return;
                case Integer.MAX_VALUE:
                    out.write(Header.INT_MAX_VALUE);
                    return;

            }
            if (((Math.abs(val) >>> 24) & 0xFF) != 0) {
                out.write(Header.INT);
                out.writeInt(val);
                return;
            }

            int neg = 0;
            if (val < 0) {
                neg = -1;
                val = -val;
            }

            //calculate N bytes
            int size = 24;
            while (((val >> size) & 0xFFL) == 0) {
                size -= 8;
            }

            //write header
            out.write(Header.INT_F1 + (size / 8) * 2 + neg);

            //write data
            while (size >= 0) {
                out.write((int) ((val >> size) & 0xFFL));
                size -= 8;
            }
        }
    };

    protected void serializeClass(DataOutput out, Class clazz) throws IOException {
        //TODO override in ElsaSerializerPojo
        out.writeUTF(clazz.getName());
    }


    private void serializeMap(int header, DataOutput out, Object obj, ElsaStack objectStack) throws IOException {
        Map<Object,Object> l = (Map) obj;
        out.write(header);
        ElsaUtil.packInt(out, l.size());
        for (Map.Entry o : l.entrySet()) {
            serialize(out, o.getKey(), objectStack);
            serialize(out, o.getValue(), objectStack);
        }
    }

    private void serializeCollection(int header, DataOutput out, Object obj, ElsaStack objectStack) throws IOException {
        Collection l = (Collection) obj;
        out.write(header);
        ElsaUtil.packInt(out, l.size());

        for (Object o : l)
            serialize(out, o, objectStack);

    }


    protected static final Ser<byte[]> SER_BYTE_ARRAY = new Ser<byte[]>() {
        @Override
        public void serialize(DataOutput out, byte[] b, ElsaStack objectStack) throws IOException {
            boolean allEqual = b.length>0;
            //check if all values in byte[] are equal
            for(int i=1;i<b.length;i++){
                if(b[i-1]!=b[i]){
                    allEqual=false;
                    break;
                }
            }
            if(allEqual){
                out.write(Header.ARRAY_BYTE_ALL_EQUAL);
                ElsaUtil.packInt(out, b.length);
                out.write(b[0]);
            }else{
                out.write(Header.ARRAY_BYTE);
                ElsaUtil.packInt(out, b.length);
                out.write(b);
            }
        }
    };


    protected static final Deser<byte[]> DESER_BYTE_ARRAY = new Deser() {
        @Override
        public byte[] deserialize(DataInput in, ElsaStack objectStack) throws IOException {
            int size = ElsaUtil.unpackInt(in);
            byte[] ret = new byte[size];
            in.readFully(ret);
            return ret;
        }
    };;

    static String deserializeString(DataInput buf, int len) throws IOException {
        char[] b = new char[len];
        for (int i = 0; i < len; i++)
            b[i] = (char) ElsaUtil.unpackInt(buf);

        return new String(b);
    }

    @Override
    public <E> E deserialize(DataInput in) throws IOException {
        return (E)deserialize(in, newElsaStack());
    }

    public Object deserialize(DataInput in, ElsaStack objectStack) throws IOException {

        final int head = in.readUnsignedByte();

        int oldObjectStackSize = objectStack.getSize();

        Object ret;
        Deser deser = headerDeser[head];
        if(deser!=null){
            ret = deser.deserialize(in, objectStack);
        }else{
            ret = deserializeUnknownHeader(in, head,objectStack);
        }

        if (head != Header.OBJECT_STACK && ret!=null && objectStack.getSize() == oldObjectStackSize) {
            //check if object was not already added to stack as part of collection
            objectStack.add(ret);
        }

        return ret;
    }


    protected Object deserializeSingleton(DataInput is, ElsaStack objectStack) throws IOException {
        int head = ElsaUtil.unpackInt(is);

        Object singleton = singletons[head];
        if(singleton == null){
                throw new IOError(new IOException("Unknown header byte, data corrupted"));
        }

        if(singleton instanceof Deser){
            singleton = ((Deser)singleton).deserialize(is,objectStack);
        }

        return singleton;
    }



    private Object[] deserializeArrayObject(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        Class clazz = loadClass2(is);
        Object[] s = (Object[]) java.lang.reflect.Array.newInstance(clazz, size);
        objectStack.add(s);
        for (int i = 0; i < size; i++){
            s[i] = deserialize(is, objectStack);
        }
        return s;
    }


    private ArrayList<Object> deserializeArrayList(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        ArrayList<Object> s = new ArrayList<Object>(size);
        objectStack.add(s);
        for (int i = 0; i < size; i++) {
            s.add(deserialize(is, objectStack));
        }
        return s;
    }


    private java.util.LinkedList deserializeLinkedList(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        java.util.LinkedList s = new java.util.LinkedList();
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.add(deserialize(is, objectStack));
        return s;
    }




    private HashSet<Object> deserializeHashSet(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        HashSet<Object> s = new HashSet<Object>(size);
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.add(deserialize(is, objectStack));
        return s;
    }


    private LinkedHashSet<Object> deserializeLinkedHashSet(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        LinkedHashSet<Object> s = new LinkedHashSet<Object>(size);
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.add(deserialize(is, objectStack));
        return s;
    }


    private TreeSet<Object> deserializeTreeSet(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);
        TreeSet<Object> s = new TreeSet<Object>();
        objectStack.add(s);
        Comparator comparator = (Comparator) deserialize(is, objectStack);
        if (comparator != null)
            s = new TreeSet<Object>(comparator);

        for (int i = 0; i < size; i++)
            s.add(deserialize(is, objectStack));
        return s;
    }


    private TreeMap<Object, Object> deserializeTreeMap(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);

        TreeMap<Object, Object> s = new TreeMap<Object, Object>();
        objectStack.add(s);
        Comparator comparator = (Comparator) deserialize(is, objectStack);
        if (comparator != null)
            s = new TreeMap<Object, Object>(comparator);
        for (int i = 0; i < size; i++)
            s.put(deserialize(is, objectStack), deserialize(is, objectStack));
        return s;
    }


    private HashMap<Object, Object> deserializeHashMap(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);

        HashMap<Object, Object> s = new HashMap<Object, Object>(size);
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.put(deserialize(is, objectStack), deserialize(is, objectStack));
        return s;
    }


    private LinkedHashMap<Object, Object> deserializeLinkedHashMap(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);

        LinkedHashMap<Object, Object> s = new LinkedHashMap<Object, Object>(size);
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.put(deserialize(is, objectStack), deserialize(is, objectStack));
        return s;
    }



    private Properties deserializeProperties(DataInput is, ElsaStack objectStack) throws IOException {
        int size = ElsaUtil.unpackInt(is);

        Properties s = new Properties();
        objectStack.add(s);
        for (int i = 0; i < size; i++)
            s.put(deserialize(is, objectStack), deserialize(is, objectStack));
        return s;
    }

    /** override this method to extend ElsaSerializerBase functionality*/
    protected void serializeUnknownObject(DataOutput out, Object obj, ElsaStack objectStack) throws IOException {
        throw new NotSerializableException("Could not serialize unknown object: "+obj.getClass().getName());
    }
    /** override this method to extend ElsaSerializerBase functionality*/
    protected Object deserializeUnknownHeader(DataInput is, int head, ElsaStack objectStack) throws IOException {
        throw new IOException("Unknown serialization header: " + head);
    }

    /**
     * Writes boolean[] into output, each value in array is represented by single byte
     *
     * @author Original author of this method is Chris Alexander, it was later optimized by Jan Kotek
     *
     * @param bool The booleans to be writen.
     */
    protected static void  writeBooleanArray(DataOutput out, boolean[] bool) throws IOException {
        int pos = 0;
        for(;pos<bool.length;){
            int v = 0;
            for(int i=0;i<8 && pos<bool.length;i++){
                v += bool[pos++]?(1<<i):0;
            }
            out.write(v);
        }
    }



    /**
     * Unpacks boolean[], each value in array is represented by single bite
     *
     * @author  author of this method is Chris Alexander, it was later optimized by Jan Kotek
     *
     * @return The boolean array decompressed from the bytes read in.
     * @throws IOException If an error occurred while reading.
     */
    protected static boolean[] readBooleanArray(int numBools,DataInput is) throws IOException {
        boolean[] ret = new boolean[numBools];
        for(int i=0;i<numBools;){
            int b = is.readUnsignedByte();
            for(int j=0;i<numBools&&j<8;j++){
                ret[i++] = ((b>>>j)&1)!=0;
            }
        }
        return ret;
    }





    /**
     * Header byte, is used at start of each record to indicate data type
     * WARNING !!! values bellow must be unique !!!!!
     *
     * @author Jan Kotek
     */
    protected interface Header {

        int ZERO_FAIL=0; //zero is invalid value, so it fails with uninitialized values
        int NULL = 1;
        int BOOLEAN_TRUE = 2;
        int BOOLEAN_FALSE = 3;

        int INT_M9 = 4;
        int INT_M8 = 5;
        int INT_M7 = 6;
        int INT_M6 = 7;
        int INT_M5 = 8;
        int INT_M4 = 9;
        int INT_M3 = 10;
        int INT_M2 = 11;
        int INT_M1 = 12;
        int INT_0 = 13;
        int INT_1 = 14;
        int INT_2 = 15;
        int INT_3 = 16;
        int INT_4 = 17;
        int INT_5 = 18;
        int INT_6 = 19;
        int INT_7 = 20;
        int INT_8 = 21;
        int INT_9 = 22;
        int INT_10 = 23;
        int INT_11 = 24;
        int INT_12 = 25;
        int INT_13 = 26;
        int INT_14 = 27;
        int INT_15 = 28;
        int INT_16 = 29;
        int INT_MIN_VALUE = 30;
        int INT_MAX_VALUE = 31;
        int INT_MF1 = 32;
        int INT_F1 = 33;
        int INT_MF2 = 34;
        int INT_F2 = 35;
        int INT_MF3 = 36;
        int INT_F3 = 37;
        int INT = 38;

        int LONG_M9 = 39;
        int LONG_M8 = 40;
        int LONG_M7 = 41;
        int LONG_M6 = 42;
        int LONG_M5 = 43;
        int LONG_M4 = 44;
        int LONG_M3 = 45;
        int LONG_M2 = 46;
        int LONG_M1 = 47;
        int LONG_0 = 48;
        int LONG_1 = 49;
        int LONG_2 = 50;
        int LONG_3 = 51;
        int LONG_4 = 52;
        int LONG_5 = 53;
        int LONG_6 = 54;
        int LONG_7 = 55;
        int LONG_8 = 56;
        int LONG_9 = 57;
        int LONG_10 = 58;
        int LONG_11 = 59;
        int LONG_12 = 60;
        int LONG_13 = 61;
        int LONG_14 = 62;
        int LONG_15 = 63;
        int LONG_16 = 64;
        int LONG_MIN_VALUE = 65;
        int LONG_MAX_VALUE = 66;

        int LONG_MF1 = 67;
        int LONG_F1 = 68;
        int LONG_MF2 = 69;
        int LONG_F2 = 70;
        int LONG_MF3 = 71;
        int LONG_F3 = 72;
        int LONG_MF4 = 73;
        int LONG_F4 = 74;
        int LONG_MF5 = 75;
        int LONG_F5 = 76;
        int LONG_MF6 = 77;
        int LONG_F6 = 78;
        int LONG_MF7 = 79;
        int LONG_F7 = 80;
        int LONG = 81;

        int BYTE_M1 = 82;
        int BYTE_0 = 83;
        int BYTE_1 = 84;
        int BYTE = 85;

        int CHAR_0 = 86;
        int CHAR_1 = 87;
        int CHAR_255 = 88;
        int CHAR = 89;

        int SHORT_M1 =90;
        int SHORT_0 = 91;
        int SHORT_1 = 92;
        int SHORT_255 = 93;
        int SHORT_M255 = 94;
        int SHORT = 95;

        int FLOAT_M1 = 96;
        int FLOAT_0 = 97;
        int FLOAT_1 = 98;
        int FLOAT_255 = 99;
        int FLOAT_SHORT = 100;
        int FLOAT = 101;

        int DOUBLE_M1 = 102;
        int DOUBLE_0 = 103;
        int DOUBLE_1 = 104;
        int DOUBLE_255 = 105;
        int DOUBLE_SHORT = 106;
        int DOUBLE_INT = 107;
        int DOUBLE = 108;

        int ARRAY_BYTE = 109;
        int ARRAY_BYTE_ALL_EQUAL = 110;

        int ARRAY_BOOLEAN = 111;
        int ARRAY_SHORT = 112;
        int ARRAY_CHAR = 113;
        int ARRAY_FLOAT = 114;
        int ARRAY_DOUBLE = 115;

        int ARRAY_INT_BYTE = 116;
        int ARRAY_INT_SHORT = 117;
        int ARRAY_INT_PACKED = 118;
        int ARRAY_INT = 119;

        int ARRAY_LONG_BYTE = 120;
        int ARRAY_LONG_SHORT = 121;
        int ARRAY_LONG_PACKED = 122;
        int ARRAY_LONG_INT = 123;
        int ARRAY_LONG = 124;

        int STRING_0 = 125;
        int STRING_1 = 126;
        int STRING_2 = 127;
        int STRING_3 = 128;
        int STRING_4 = 129;
        int STRING_5 = 130;
        int STRING_6 = 131;
        int STRING_7 = 132;
        int STRING_8 = 133;
        int STRING_9 = 134;
        int STRING_10 = 135;
        int STRING = 136;

        int BIGDECIMAL = 137;
        int BIGINTEGER = 138;


        int CLASS = 139;
        int DATE = 140;
        int UUID = 141;
        int USER_DESER = 142;

        //142 to 158 reserved for other non recursive objects

        int SINGLETON = 159;
        int  ARRAY_OBJECT = 160;
        int ARRAY_OBJECT_ALL_NULL = 161;
        int ARRAY_OBJECT_NO_REFS = 162;

        int  ARRAYLIST = 163;
        int  TREEMAP = 164;
        int  HASHMAP = 165;
        int  LINKEDHASHMAP = 166;
        int  TREESET = 167;
        int  HASHSET = 168;
        int  LINKEDHASHSET = 169;
        int  LINKEDLIST = 170;
        int  PROPERTIES = 171;

        /**
         * Value used in Java Serialization header. For this header we throw an exception because data might be corrupted
         */
        int JAVA_SERIALIZATION = 172;

        /**
         * Use POJO Serializer to get class structure and set its fields.
         * Class Info is fetched from ElsaClassInfoResolver
         */
        int POJO_RESOLVER = 173;

        /**
         * used for reference to already serialized object in object graph
         */
        int OBJECT_STACK = 174;

        /**
         * Use POJO Serializer to get class structure and set its fields.
         * Class Info is stored in local stream
         */
        int POJO = 175;

        /** Class Info stored in local stream */
        int POJO_CLASSINFO = 176;
    }

    /** return true if mapdb knows howto serialize given object*/
    public boolean isSerializable(Object o) {
        //check if is known singleton
        if(singletonsReverse.containsKey(o)) {
            return true;
        }

        //check list of classes
        if(ser.containsKey(o.getClass())) {
            return true;
        }

        return false;
    }

}
