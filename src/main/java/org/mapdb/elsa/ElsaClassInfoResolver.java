package org.mapdb.elsa;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jan on 4/18/16.
 */
public interface ElsaClassInfoResolver {

    ElsaClassInfoResolver VOID = new ElsaClassInfoResolver() {
        @Override
        public ElsaSerializerPojo.ClassInfo getClassInfo(int classId) {
            return null;
        }

        @Override
        public int classToId(String className) {
            return -1;
        }
    };

    public class ArrayBased implements ElsaClassInfoResolver {

        protected final ElsaSerializerPojo.ClassInfo[] classInfos;
        protected final Map<String, Integer> reverse = new HashMap();

        public ArrayBased(Class[] classes) {
            classInfos = new ElsaSerializerPojo.ClassInfo[classes.length];
            for(int i=0;i<this.classInfos.length;i++){
                classInfos[i] = ElsaSerializerPojo.makeClassInfo(classes[i]);
                reverse.put(this.classInfos[i].name, i);
            }
        }

        public ArrayBased(ElsaSerializerPojo.ClassInfo[] classInfos) {
            this.classInfos = classInfos.clone();
            for(int i=0;i<this.classInfos.length;i++){
                reverse.put(this.classInfos[i].name, i);
            }
        }

        @Override
        public ElsaSerializerPojo.ClassInfo getClassInfo(int classId) {
            return classInfos[classId];
        }

        @Override
        public int classToId(String className) {
            Integer ret =  reverse.get(className);
            return ret!=null?ret : -1;
        }
    }

    ElsaSerializerPojo.ClassInfo getClassInfo(int classId);

    int classToId(String className);
}
