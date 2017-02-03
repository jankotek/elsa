package org.mapdb.elsa;

/**
 * Callback interface to notify user that unknown class was serialized.
 * User can add missing classes to Class Catalog using {@link ElsaMaker#registerClasses(Class[])}
 */
public interface ElsaClassCallback {

    /** default implementation, does nothing */
    ElsaClassCallback VOID = new ElsaClassCallback() {
        @Override
        public void classMissing(Class clazz) {
        }
    };


    /** called by Elsa when an unknown class is found during serialization
     * @param clazz unknown class
     */
    void classMissing(Class clazz);

}
