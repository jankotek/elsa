package org.mapdb.elsa;

import java.io.IOException;

/**
 * Created by jan on 3/28/16.
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
}
