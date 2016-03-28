package org.mapdb.elsa;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * Created by jan on 3/28/16.
 */
public final class ObjectOutputStream2 extends ObjectOutputStream {

    private SerializerPojo serializerPojo;
    private final SerializerPojo.ClassInfo[] classes;

    protected ObjectOutputStream2(SerializerPojo serializerPojo, OutputStream out, SerializerPojo.ClassInfo[] classes) throws IOException, SecurityException {
        super(out);
        this.serializerPojo = serializerPojo;
        this.classes = classes;
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        int classId = SerializerPojo.classToId(classes, desc.getName());
        ElsaUtil.packInt(this, classId);
        if (classId == -1) {
            //unknown class, write its full name
            this.writeUTF(desc.getName());
            //and notify about unknown class

            serializerPojo.notifyMissingClassInfo(desc.getName());
        }
    }
}
