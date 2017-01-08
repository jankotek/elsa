package org.mapdb.elsa;

import java.util.HashMap;
import java.util.Map;


/**
 * In binary data Class Names are replaced by `Integer` IDs.
 * This resolver maps Class Name to its ID and back.
 * <p/>
 * This way you can implement your own way to store class information.
 * For example MapDB has custom Class Info resolver to store Class Infos in Class Catalog.
 */
public interface ElsaClassInfoResolver {

    /** does not resolve any class info, always returns void */
    public ElsaClassInfoResolver VOID = new ElsaClassInfoResolver() {
        @Override
        public ElsaSerializerPojo.ClassInfo getClassInfo(int classId) {
            return null;
        }

        @Override
        public int classToId(String className) {
            return -1;
        }
    };


    /**
     * Stores Class Names in sequential array.
     * Classes must always be registered in the same order, or their ID will change.
     */
    public class ArrayBased implements ElsaClassInfoResolver {

        protected final ElsaSerializerPojo.ClassInfo[] classInfos;
        protected final Map<String, Integer> reverse = new HashMap();

        /**
         * Registers set of classes with their classloader.
         *
         * @param classes classes used in resolver, index in array is class ID in binary data
         * @param classLoader used to load classes from their name
         */
        public ArrayBased(Class[] classes, ClassLoader classLoader) {
            classInfos = new ElsaSerializerPojo.ClassInfo[classes.length];
            for(int i=0;i<this.classInfos.length;i++){
                classInfos[i] = ElsaSerializerPojo.makeClassInfo(classes[i], classLoader);
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

    /**
     * Resolves Integer Class ID (used in binary data) into Class Info which contains  class name, fields name, fields order...
     *
     * @param classId
     * @return
     */
    ElsaSerializerPojo.ClassInfo getClassInfo(int classId);

    /**
     * Resolves Class Name to its Class ID (used in binary data)/
     * @param className
     * @return Class ID, it is used in serialized binary data to identify class
     */
    int classToId(String className);
}
