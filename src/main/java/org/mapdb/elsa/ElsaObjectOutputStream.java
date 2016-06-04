package org.mapdb.elsa;

import java.io.*;

/**
 * Created by jan on 4/17/16.
 */
public class ElsaObjectOutputStream extends ObjectOutputStream {

    protected final DataOutput out;
    protected final ElsaSerializerPojo serializer;

    public ElsaObjectOutputStream(DataOutput out, ElsaSerializerPojo serializer) throws IOException {
        super();
        this.out = out;
        this.serializer = serializer;
    }

    public ElsaObjectOutputStream(OutputStream out) throws IOException {
        this(new DataOutputStream(out), new ElsaSerializerPojo());
    }

    @Override
    protected void writeObjectOverride(Object obj) throws IOException {
        serializer.serialize(out, obj);
    }

    @Override
    public void close() throws IOException {
        if(out instanceof Closeable)
            ((Closeable)out).close();
    }
}
