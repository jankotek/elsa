package org.mapdb.elsa;

import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Created by jan on 3/28/16.
 */
public final class ObjectInputStream2 extends ObjectInputStream {

    private SerializerPojo serializerPojo;
    private final SerializerPojo.ClassInfo[] classes;

    // One-element cache to handle the common case where we immediately resolve a descriptor to its class.
    // Unlike most ObjecTInputStream subclasses we actually have to look up the class to find the descriptor!
    private ObjectStreamClass lastDescriptor;
    private Class lastDescriptorClass;

    protected ObjectInputStream2(SerializerPojo serializerPojo, DataInput in, SerializerPojo.ClassInfo[] classes) throws IOException, SecurityException {
        super(new ElsaUtil.DataInputToStream(in));
        this.serializerPojo = serializerPojo;
        this.classes = classes;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int classId = ElsaUtil.unpackInt(this);

        final Class clazz;
        String className;
        if (classId == -1) {
            //unknown class, so read its name
            className = this.readUTF();
        } else {
            className = classes[classId].name;
        }
        clazz = serializerPojo.classLoader.run(className);
        final ObjectStreamClass descriptor = ObjectStreamClass.lookup(clazz);

        lastDescriptor = descriptor;
        lastDescriptorClass = clazz;

        return descriptor;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (desc == lastDescriptor) return lastDescriptorClass;
        Class<?> clazz = serializerPojo.classLoader.run(desc.getName());
        if (clazz != null)
            return clazz;
        return super.resolveClass(desc);
    }
}
