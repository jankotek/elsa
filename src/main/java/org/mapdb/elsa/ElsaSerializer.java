package org.mapdb.elsa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by jan on 5/15/16.
 */
public interface ElsaSerializer {
    void serialize(DataOutput out, Object obj) throws IOException;

    <E> E deserialize(DataInput in) throws IOException;
}
