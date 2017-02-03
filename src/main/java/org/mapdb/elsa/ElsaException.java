package org.mapdb.elsa;

/**
 * General Elsa exception which wraps checked Exceptipns.
 */
//TODO exception hierarchy
public class ElsaException extends RuntimeException {

    public ElsaException() {
        super();
    }

    public ElsaException(String s) {
        super(s);
    }

    public ElsaException(Exception s) {
        super(s);
    }

    /**
     * An exception thrown when unknown Header Byte is found during deserialization.
     * This could mean:
     * <ul>
     *     <li>Data were corrupted</li>
     *     <li>Newer Elsa version was used for serialization, older Elsa is used for deserialization, older version can not read new data types</li>
     *     <li>User registered extra serializers at serialization, but those were not restored at deserialization</li>
     *     <li>User registered extra singletongs at serialization, but those were not restored at deserialization</li>
     * </ul>
     *
     */
    public static class UnknownHeaderByte extends ElsaException{
        public UnknownHeaderByte(String msg){
            super(msg);
        }

    }

    /** Unchecked version of {@link java.lang.ClassNotFoundException} */
    public static class ClassNotFound extends ElsaException{


        public ClassNotFound(ClassNotFoundException e) {
            super(e);
        }
    }
}
