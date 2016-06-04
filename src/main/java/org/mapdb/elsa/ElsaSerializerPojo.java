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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Serializer which handles POJO, object graphs etc.
 *
 * @author  Jan Kotek
 */
public class ElsaSerializerPojo extends ElsaSerializerBase implements Serializable{

    private static final Logger LOG = Logger.getLogger(ElsaSerializerPojo.class.getName());
   // public static final ClassInfo[] EMPTY_CLASS_INFOS = new ClassInfo[0];

    static{
        String ver = System.getProperty("java.version");
        if(ver!=null && ver.toLowerCase().contains("jrockit")){
            LOG.warning("Elsa POJO serialization might not work on JRockit JVM. See https://github.com/jankotek/mapdb/issues/572");
        }
    }

    protected final ClassCallback missingClassNotification;
    protected final ClassInfoResolver classInfoResolver;

    public ElsaSerializerPojo(){
        this(0, null, null,  null, null, null, null);
    }

    public ElsaSerializerPojo(
            int objectStackType,
            Object[] singletons,
            Map<Class, Ser> userSer,
            Map<Class, Integer> userSerHeaders,
            Map<Integer, Deser> userDeser,
            ClassCallback missingClassNotification,
            ClassInfoResolver classInfoResolver){
        super(objectStackType, singletons, userSer, userSerHeaders, userDeser);
        this.missingClassNotification = missingClassNotification!=null?missingClassNotification:ClassCallback.VOID;
        this.classInfoResolver = classInfoResolver!=null?classInfoResolver:ClassInfoResolver.VOID;
    }

    public void classInfoSerialize(DataOutput out, ClassInfo ci) throws IOException {
        out.writeUTF(ci.name);
        out.writeBoolean(ci.isEnum);
        out.writeBoolean(ci.useObjectStream);
        if(ci.useObjectStream)
            return; //no fields

        ElsaUtil.packInt(out, ci.fields.length);
        for (FieldInfo fi : ci.fields) {
            out.writeUTF(fi.name);
            out.writeBoolean(fi.primitive);
            out.writeUTF(fi.type);
        }
    }

    public ClassInfo classInfoDeserialize(DataInput in) throws IOException{
        String className = in.readUTF();
        Class clazz = null;
        boolean isEnum = in.readBoolean();
        boolean isExternalizable = in.readBoolean();

        int fieldsNum = isExternalizable? 0 : ElsaUtil.unpackInt(in);
        FieldInfo[] fields = new FieldInfo[fieldsNum];
        for (int j = 0; j < fieldsNum; j++) {
            String fieldName = in.readUTF();
            boolean primitive = in.readBoolean();
            String type = in.readUTF();
            if(clazz == null)
                clazz = loadClass2(className);

            fields[j] = new FieldInfo(fieldName,
                    type,
                    primitive?null:loadClass2(type),
                    clazz);
        }
        return new ClassInfo(className, fields,isEnum,isExternalizable);
    }


    private static final long serialVersionUID = 1290400014981859025L;


    protected static Class classForName(String className, ClassLoader loader) {
        try {
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            throw new ElsaException(e);
        }
    }

    protected ClassInfo getClassInfo(int classId){
        if(classId<0)
            return null;
        return classInfoResolver.getClassInfo(classId);
    }

    protected void notifyMissingClassInfo(Class className){
        missingClassNotification.classMissing(className);
    }



    /**
     * Stores info about single class stored in MapDB.
     * Roughly corresponds to 'java.io.ObjectStreamClass'
     */
    public static final class ClassInfo {

        //PERF optimize deserialization cost here.

        public final String name;
        public final FieldInfo[] fields;
        public final Map<String, FieldInfo> name2fieldInfo = new HashMap<String, FieldInfo>();
        public final Map<String, Integer> name2fieldId = new HashMap<String, Integer>();
        public ObjectStreamField[] objectStreamFields;

        public final boolean isEnum;

        public final boolean useObjectStream;

        public ClassInfo(final String name, final FieldInfo[] fields, final boolean isEnum, final boolean isExternalizable) {
            this.name = name;
            this.isEnum = isEnum;
            this.useObjectStream = isExternalizable;

            this.fields = fields.clone();

            //TODO constructing dictionary might be contraproductive, perhaps use linear scan for smaller sizes
            for (int i=0;i<fields.length;i++) {
                FieldInfo f = fields[i];
                this.name2fieldId.put(f.name, i);
                this.name2fieldInfo.put(f.name, f);
            }
        }

        public int getFieldId(String name) {
            Integer fieldId = name2fieldId.get(name);
            if(fieldId != null)
                return fieldId;
            return -1;
        }

        public ObjectStreamField[] getObjectStreamFields() {
            return objectStreamFields;
        }


        @Override public String toString(){
            return super.toString()+ "["+name+"]";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClassInfo classInfo = (ClassInfo) o;

            if (isEnum != classInfo.isEnum) return false;
            if (useObjectStream != classInfo.useObjectStream) return false;
            if (name != null ? !name.equals(classInfo.name) : classInfo.name != null) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(fields, classInfo.fields);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (fields != null ? Arrays.hashCode(fields) : 0);
            result = 31 * result + (isEnum ? 1 : 0);
            result = 31 * result + (useObjectStream ? 1 : 0);
            return result;
        }
    }

    /**
     * Stores info about single field stored in MapDB.
     * Roughly corresponds to 'java.io.ObjectFieldClass'
     */
    public static class FieldInfo {
        public final String name;
        public final boolean primitive;
        public final String type;
        public Class<?> typeClass;
        // Class containing this field
        public final Class<?> clazz;
        public Field field;

//        FieldInfo(String name, boolean primitive, String type, Class<?> clazz) {
//            this(name, primitive, ElsaSerializerPojo.classForNameClassLoader(), type, clazz);
//        }
//
//        public FieldInfo(String name, boolean primitive, ClassLoader classLoader, String type, Class<?> clazz) {
//            this(name, type, primitive ? null : classForName(classLoader, type), clazz);
//        }
//
//        public FieldInfo(ObjectStreamField sf, ClassLoader loader, Class<?> clazz) {
//            this(sf.getName(), sf.isPrimitive(), loader, sf.getType().getName(), clazz);
//        }

        public FieldInfo(String name, String type, Class<?> typeClass, Class<?> clazz) {
            this.name = name;
            this.primitive = typeClass == null;
            this.type = type;
            this.clazz = clazz;
            this.typeClass = typeClass;

            //init field

            Class<?> aClazz = clazz;

            // iterate over class hierarchy, until root class
            while (true) {
                if(aClazz == Object.class) throw new RuntimeException("Could not set field value: "+name+" - "+clazz.toString());
                // access field directly
                try {
                    Field f = aClazz.getDeclaredField(name);
                    // security manager may not be happy about this
                    if (!f.isAccessible())
                        f.setAccessible(true);
                    field = f;
                    break;
                } catch (NoSuchFieldException e) {
                    //field does not exists
                }
                // move to superclass
                aClazz = aClazz.getSuperclass();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldInfo fieldInfo = (FieldInfo) o;

            if (primitive != fieldInfo.primitive) return false;
            if (name != null ? !name.equals(fieldInfo.name) : fieldInfo.name != null) return false;
            if (type != null ? !type.equals(fieldInfo.type) : fieldInfo.type != null) return false;
            if (typeClass != null ? !typeClass.equals(fieldInfo.typeClass) : fieldInfo.typeClass != null) return false;
            if (clazz != null ? !clazz.equals(fieldInfo.clazz) : fieldInfo.clazz != null) return false;
            return !(field != null ? !field.equals(fieldInfo.field) : fieldInfo.field != null);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (primitive ? 1 : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (typeClass != null ? typeClass.hashCode() : 0);
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            result = 31 * result + (field != null ? field.hashCode() : 0);
            return result;
        }
    }


    public static ClassInfo makeClassInfo(Class clazz){
        final boolean advancedSer = usesAdvancedSerialization(clazz);
        ObjectStreamField[] streamFields = advancedSer ? new ObjectStreamField[0] : makeFieldsForClass(clazz);
        FieldInfo[] fields = new FieldInfo[streamFields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField sf = streamFields[i];
            String type = sf.getType().getName();
            fields[i] = new FieldInfo(
                    sf.getName(),
                    type,
                    sf.isPrimitive() ? null : loadClass3(type, Thread.currentThread().getContextClassLoader()),
                    clazz);
        }

        return new ClassInfo(clazz.getName(), fields, clazz.isEnum(), advancedSer);
    }


    public ClassInfo makeClassInfo(String className){
        Class clazz = loadClass2(className);
        final boolean advancedSer = usesAdvancedSerialization(clazz);
        ObjectStreamField[] streamFields = advancedSer ? new ObjectStreamField[0] : makeFieldsForClass(clazz);
        FieldInfo[] fields = new FieldInfo[streamFields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField sf = streamFields[i];
            String type = sf.getType().getName();
            fields[i] = new FieldInfo(
                    sf.getName(),
                    type,
                    sf.isPrimitive() ? null : loadClass2(type),
                    clazz);
        }

        return new ClassInfo(clazz.getName(), fields, clazz.isEnum(), advancedSer);
    }

    protected static boolean usesAdvancedSerialization(Class<?> clazz) {
        if(Externalizable.class.isAssignableFrom(clazz))
            return true;
        try {
            if(clazz.getDeclaredMethod("readObject",ObjectInputStream.class)!=null)
                return true;
        } catch (NoSuchMethodException e) {
        }

        try {
            if(clazz.getDeclaredMethod("writeObject",ObjectOutputStream.class)!=null)
                return true;
        } catch (NoSuchMethodException e) {
        }

        try {
            if(clazz.getDeclaredMethod("writeReplace")!=null)
                return true;
        } catch (NoSuchMethodException e) {
        }

        try {
            if(clazz.getDeclaredMethod("readResolve")!=null)
                return true;
        } catch (NoSuchMethodException e) {
        }

        Class su = clazz.getSuperclass();
        if(su==Object.class || su==null)
            return false;
        return usesAdvancedSerialization(su);
    }


    protected ObjectStreamField[] fieldsForClass(Class<?> clazz) {
        ObjectStreamField[] fields = null;
        ClassInfo classInfo = null;
        int classId = classToId(clazz.getName());
        if (classId != -1) {
            classInfo = getClassInfo(classId);
            fields = classInfo.getObjectStreamFields();
        }
        if (fields == null) {
            fields = makeFieldsForClass(clazz);
        }
        return fields;
    }

    private static ObjectStreamField[] makeFieldsForClass(Class<?> clazz) {
        ObjectStreamField[] fields = new ObjectStreamField[4];
        int fieldsSize = 0;
        ObjectStreamClass streamClass = ObjectStreamClass.lookup(clazz);
        while (streamClass != null) {
            for (ObjectStreamField f : streamClass.getFields()) {
                if(fieldsSize==fields.length-1)
                    fields = Arrays.copyOf(fields, fields.length*2);
                fields[fieldsSize++]=f;
            }
            clazz = clazz.getSuperclass();
            streamClass = clazz!=null? ObjectStreamClass.lookup(clazz) : null;
        }
        fields = Arrays.copyOf(fields, fieldsSize);
        //TODO what is StreamField? perhaps performance optim?
//        if(classInfo != null)
//            classInfo.setObjectStreamFields(fields);
        return fields;
    }

    public boolean isSerializable(Object o){
        if(super.isSerializable(o))
            return true;

        return Serializable.class.isAssignableFrom(o.getClass());
    }

    protected void assertClassSerializable(Class<?> clazz) throws NotSerializableException, InvalidClassException {
        if(classToId(clazz.getName())!=-1)
            return;

        if (!Serializable.class.isAssignableFrom(clazz))
            throw new NotSerializableException(clazz.getName());

    }


    public Object getFieldValue(FieldInfo fieldInfo, Object object) {

        if(fieldInfo.field==null){
            throw new NoSuchFieldError(object.getClass() + "." + fieldInfo.name);
        }


        try {
            return fieldInfo.field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not get value from field", e);
        }
    }



    public void setFieldValue(FieldInfo fieldInfo, Object object, Object value) {
        if(fieldInfo.field==null)
            throw new NoSuchFieldError(object.getClass() + "." + fieldInfo.name);

        try{
           fieldInfo.field.set(object, value);
        } catch (IllegalAccessException e) {
           throw new RuntimeException("Could not set field value: ",e);
        }

    }


    public int classToId(String className) {
        return classInfoResolver.classToId(className);
    }

    @Override
    protected void serializeUnknownObject(DataOutput out, Object obj, ElsaStack objectStack) throws IOException {
        assertClassSerializable(obj.getClass());

        int head = Header.POJO;
        ClassInfo classInfo;

        //try to resolve from global class resolver
        int classId = classToId(obj.getClass().getName());
        if(classId>=0){
            head = Header.POJO_RESOLVER;
            classInfo = getClassInfo(classId);
        }else if((classId = objectStack.resolveClassId(obj.getClass().getName())) <0) {
            //class is not known
            notifyMissingClassInfo(obj.getClass());
            classInfo = makeClassInfo(obj.getClass());

            //write unknown class info into local class catalog
            classId = objectStack.addClassInfo(classInfo);
            out.write(Header.POJO_CLASSINFO);
            ElsaUtil.packInt(out, classId);
            classInfoSerialize(out, classInfo);
        }else{
            //classId is known in stream, get it from object stack
            classInfo = objectStack.resolveClassInfo(classId);
        }
        out.write(head);
        //write class header
        ElsaUtil.packInt(out, classId);
        //and rest of the data

        if(classInfo.useObjectStream){
            ObjectOutputStream2 out2 = new ObjectOutputStream2(this, (OutputStream) out);
            out2.writeObject(obj);
            return;
        }

        if(classInfo.isEnum) {
            int ordinal = ((Enum<?>)obj).ordinal();
            ElsaUtil.packInt(out, ordinal);
        }

        ObjectStreamField[] fields = fieldsForClass(obj.getClass());
        ElsaUtil.packInt(out, fields.length);

        for (ObjectStreamField f : fields) {
            //write field ID
            int fieldId = classInfo.getFieldId(f.getName());
            if (fieldId == -1) {
                throw new AssertionError("Missing field: "+f.getName());
                //TODO class info is immutable in 2.0, so this old code can not be used
//                //field does not exists in class definition stored in db,
//                //probably new field was added so add field descriptor
//                fieldId = classInfo.addFieldInfo(new FieldInfo(f, clazz));
//                saveClassInfo();
            }
            ElsaUtil.packInt(out, fieldId);
            //and write value
            Object fieldValue = getFieldValue(classInfo.fields[fieldId], obj);
            serialize(out, fieldValue, objectStack);
        }
    }

    @Override
    protected Object deserializeUnknownHeader(DataInput in, int head, ElsaStack objectStack) throws IOException {

        if(head==Header.POJO_CLASSINFO){
            int classId = ElsaUtil.unpackInt(in);
            ClassInfo classInfo = classInfoDeserialize(in);
            int classId2 = objectStack.addClassInfo(classInfo);
            if(classId!=classId2)
                throw new ElsaException("Wrong Stream ClassInfo order");
            return deserialize(in, objectStack);
        }
        if(head!= Header.POJO_RESOLVER && head!= Header.POJO)
            throw new ElsaException("wrong header");
        try {
            int classId = ElsaUtil.unpackInt(in);
            ClassInfo classInfo =
                    head==Header.POJO_RESOLVER
                            ? getClassInfo(classId)
                            : objectStack.resolveClassInfo(classId);


            //is unknown Class or uses specialized serialization
            if (classId == -1 || classInfo.useObjectStream) {
                //deserialize using object stream
                ObjectInputStream2 in2 = new ObjectInputStream2(this, wrapStream(in));
                Object o = in2.readObject();
                objectStack.add(o);
                return o;
            }

            Class<?> clazz = loadClass(classInfo.name);
            if (!Serializable.class.isAssignableFrom(clazz))
                throw new NotSerializableException(clazz.getName());

            Object o;
            if (classInfo.isEnum) {
                int ordinal = ElsaUtil.unpackInt(in);
                o = clazz.getEnumConstants()[ordinal];
            } else {
                o = createInstanceSkippinkConstructor(clazz);
            }

            objectStack.add(o);


            int fieldCount = ElsaUtil.unpackInt(in);
            for (int i = 0; i < fieldCount; i++) {
                int fieldId = ElsaUtil.unpackInt(in);
                FieldInfo f = classInfo.fields[fieldId];
                Object fieldValue = deserialize(in, objectStack);
                setFieldValue(f, o, fieldValue);
            }

            return o;
        }catch(ClassNotFoundException e){
            throw new ElsaException(e);
        }
    }

    private InputStream wrapStream(DataInput in) {
        if(in instanceof InputStream)
            return (InputStream) in;
        return new ElsaUtil.DataInputToStream(in);
    }


    static protected Method sunConstructor = null;
    static protected Object sunReflFac = null;
    static protected Method androidConstructor = null;
    static private Method androidConstructorGinger = null;
    static private Method androidConstructorJelly = null;
    static private Object constructorId;

    static{
        try{
            Class<?> clazz = loadClass3("sun.reflect.ReflectionFactory", Thread.currentThread().getContextClassLoader());
            if(clazz!=null){
                Method getReflectionFactory = clazz.getMethod("getReflectionFactory");
                sunReflFac = getReflectionFactory.invoke(null);
                sunConstructor = clazz.getMethod("newConstructorForSerialization",
                        java.lang.Class.class, java.lang.reflect.Constructor.class);
            }
        }catch(Exception e){
            //ignore
        }

        if(sunConstructor == null)try{
            //try android way
            Method newInstance = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Class.class);
            newInstance.setAccessible(true);
            androidConstructor = newInstance;

        }catch(Exception e){
            //ignore
        }

        //this method was taken from 
        //http://dexmaker.googlecode.com/git-history/5a7820356e68a977711afc854d6cd71296c56391/src/mockito/java/com/google/dexmaker/mockito/UnsafeAllocator.java
        //Copyright (C) 2012 The Android Open Source Project, licenced under Apache 2 license
        if(sunConstructor == null && androidConstructor == null)try{
            //try android post ginger way
            Method getConstructorId = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
            getConstructorId.setAccessible(true);
            constructorId = getConstructorId.invoke(null, Object.class);

            Method newInstance = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, getConstructorId.getReturnType());
            newInstance.setAccessible(true);
            androidConstructorGinger = newInstance;

        }catch(Exception e){
            //ignore
        }

        if(sunConstructor == null && androidConstructor == null && androidConstructorGinger == null)try{
            //try android post 4.2 way
            Method getConstructorId = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
            getConstructorId.setAccessible(true);
            constructorId = getConstructorId.invoke(null, Object.class);

            Method newInstance = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, long.class);
            newInstance.setAccessible(true);
            androidConstructorJelly = newInstance;

        }catch(Exception e){
            //ignore
        }
    }


    protected static Map<Class<?>, Constructor<?>> class2constuctor = new ConcurrentHashMap<Class<?>, Constructor<?>>();

    /**
     * <p>
     * For pojo serialization we need to instantiate class without invoking its constructor.
     * There are two ways to do it:
     * </p><p>
     *   Using proprietary API on Oracle JDK and OpenJDK
     *   sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization()
     *   more at http://www.javaspecialists.eu/archive/Issue175.html
     * </p><p>
     *   Using {@code ObjectInputStream.newInstance} on Android
     *   http://stackoverflow.com/a/3448384
     * </p><p>
     *   If non of these works we fallback into usual reflection which requires an no-arg constructor
     * </p>
     */
    @SuppressWarnings("restriction")
	protected <T> T createInstanceSkippinkConstructor(Class<T> clazz) {

        try {
            if (sunConstructor != null) {
                //Sun specific way
                Constructor<?> intConstr = class2constuctor.get(clazz);

                if (intConstr == null) {
                    Constructor<?> objDef = Object.class.getDeclaredConstructor();
                    intConstr = (Constructor<?>) sunConstructor.invoke(sunReflFac, clazz, objDef);
                    class2constuctor.put(clazz, intConstr);
                }

                return (T) intConstr.newInstance();
            } else if (androidConstructor != null) {
                //android (harmony) specific way
                return (T) androidConstructor.invoke(null, clazz, Object.class);
            } else if (androidConstructorGinger != null) {
                //android (post ginger) specific way
                return (T) androidConstructorGinger.invoke(null, clazz, constructorId);
            } else if (androidConstructorJelly != null) {
                //android (post 4.2) specific way
                return (T) androidConstructorJelly.invoke(null, clazz, constructorId);
            } else {
                //try usual generic stuff which does not skip constructor
                Constructor<?> c = class2constuctor.get(clazz);
                if (c == null) {
                    c = clazz.getConstructor();
                    if (!c.isAccessible()) c.setAccessible(true);
                    class2constuctor.put(clazz, c);
                }
                return (T) c.newInstance();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }


}