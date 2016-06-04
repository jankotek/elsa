package org.mapdb.elsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Created by jan on 3/28/16.
 */
final class ObjectInputStream2 extends ObjectInputStream {

    private ElsaSerializerPojo serializerPojo;

    // One-element cache to handle the common case where we immediately resolve a descriptor to its class.
    // Unlike most ObjecTInputStream subclasses we actually have to look up the class to find the descriptor!
    private ObjectStreamClass lastDescriptor;
    private Class lastDescriptorClass;

    protected ObjectInputStream2(ElsaSerializerPojo serializerPojo, InputStream in) throws IOException, SecurityException {
        super(in);
        this.serializerPojo = serializerPojo;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int classId = ElsaUtil.unpackInt((InputStream)this);

        final Class clazz;
        String className;
        if (classId == -1) {
            //unknown class, so read its name
            className = this.readUTF();
        } else {
            className =serializerPojo.classInfoResolver.getClassInfo(classId).name;
        }
        clazz = serializerPojo.loadClass(className);
        final ObjectStreamClass descriptor = ObjectStreamClass.lookup(clazz);

        lastDescriptor = descriptor;
        lastDescriptorClass = clazz;

        return descriptor;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (desc == lastDescriptor) return lastDescriptorClass;
        Class<?> clazz = serializerPojo.loadClass(desc.getName());
        if (clazz != null)
            return clazz;
        return super.resolveClass(desc);
    }
}
