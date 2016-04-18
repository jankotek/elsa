package org.mapdb.elsa;

/**
 * Created by jan on 4/18/16.
 */
public interface ClassInfoResolver {

    ClassInfoResolver VOID = new ClassInfoResolver() {
        @Override
        public SerializerPojo.ClassInfo getClassInfo(int classId) {
            return null;
        }

        @Override
        public int classToId(String className) {
            return -1;
        }
    };

    SerializerPojo.ClassInfo getClassInfo(int classId);

    int classToId(String className);
}
