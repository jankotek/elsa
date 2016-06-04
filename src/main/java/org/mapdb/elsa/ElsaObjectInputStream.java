package org.mapdb.elsa;

import java.io.*;

/**
 * Created by jan on 4/17/16.
 */
public class ElsaObjectInputStream extends ObjectInputStream {

    protected final DataInput input;
    protected ElsaSerializerPojo serializer;


    public ElsaObjectInputStream(InputStream stream) throws IOException {
        this(new DataInputStream(stream), new ElsaSerializerPojo());
    }

    public ElsaObjectInputStream(DataInput input, ElsaSerializerPojo serializer) throws IOException {
        super();
        this.input = input;
        this.serializer = serializer;
    }

    @Override
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return serializer.deserialize(input);
    }

    @Override
    public void close() throws IOException {
        if(input instanceof Closeable)
            ((Closeable)input).close();
    }
}
