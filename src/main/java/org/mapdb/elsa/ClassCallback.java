package org.mapdb.elsa;

/**
 * Created by jan on 4/17/16.
 */
public interface ClassCallback {

    ClassCallback VOID = new ClassCallback() {
        @Override
        public void classMissing(String className) {
        }
    };

    void classMissing(String className);

}
