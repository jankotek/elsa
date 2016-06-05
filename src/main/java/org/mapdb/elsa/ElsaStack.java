package org.mapdb.elsa;

import java.util.*;

/**
 * Utility class similar to ArrayList, but with fast identity search.
 */
public abstract class ElsaStack{


    public static final class IdentityArray extends ElsaStack{

        private int size ;
        private Object[] data ;

        public IdentityArray(){
            size=0;
            data = new Object[1];
        }

        public boolean forwardRefs = false;


        public void add(Object o) {
            if (data.length == size) {
                //grow array if necessary
                data = Arrays.copyOf(data, data.length * 2);
            }

            data[size] = o;
            size++;
        }



        /**
         * This method is reason why ArrayList is not used.
         * Search an item in list and returns its index.
         * It uses identity rather than 'equalsTo'
         * One could argue that TreeMap should be used instead,
         * but we do not expect large object trees.
         * This search is VERY FAST compared to Maps, it does not allocate
         * new instances or uses method calls.
         *
         * @param obj to find in list
         * @return index of object in list or -1 if not found
         */
        public int identityIndexOf(Object obj) {
            for (int i = 0; i < size; i++) {
                if (obj == data[i]){
                    forwardRefs = true;
                    return i;
                }
            }
            return -1;
        }

        public int getSize() {
            return size;
        }

        public Object getInstance(int i) {
            return data[i];
        }

    }


    public static final class MapStack extends ElsaStack{

        final Map<Object, Integer> data;
        private final List<Object> reverse = new ArrayList<Object>();

        public MapStack(Map<Object, Integer> data) {
            this.data = data;
        }

        @Override
        public void add(Object o) {
            int size = data.size();
            data.put(o, size);
            reverse.add(o);
        }

        @Override
        public int identityIndexOf(Object obj) {
            Integer ret = data.get(obj);
            return ret==null ? -1 : ret;
        }

        @Override
        public int getSize() {
            return reverse.size();
        }

        @Override
        public Object getInstance(int i) {
            return reverse.get(i);
        }
    }


    public static final class NoReferenceStack extends ElsaStack{

        @Override
        public void add(Object o) {
        }

        @Override
        public int identityIndexOf(Object obj) {
            return -1;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public Object getInstance(int i) {
            throw new UnsupportedOperationException();
        }
    }


    public abstract void add(Object o);
    public abstract int identityIndexOf(Object obj);
    public abstract int getSize();
    public abstract Object getInstance(int i);


    private ElsaSerializerPojo.ClassInfo[] classInfos = null;

    public int resolveClassId(String clazzName) {
        if(classInfos==null)
            return -1;
        for(int i=0;i<classInfos.length;i++){
            if(classInfos[i].name.equals(clazzName))
                return i;
        }
        return -1;
    }

    public int addClassInfo(ElsaSerializerPojo.ClassInfo clazzInfo){
        if(classInfos==null)
            classInfos = new ElsaSerializerPojo.ClassInfo[0];

        int size = classInfos.length;
        classInfos = Arrays.copyOf(classInfos, size+1);
        classInfos[size] = clazzInfo;
        return size;
    }

    public ElsaSerializerPojo.ClassInfo resolveClassInfo(int classId) {
        return classInfos[classId];
    }

}
