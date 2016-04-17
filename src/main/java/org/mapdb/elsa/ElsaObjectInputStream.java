package org.mapdb.elsa;

import java.io.*;

/**
 * Created by jan on 4/17/16.
 */
public class ElsaObjectInputStream extends ObjectInputStream {

    protected final DataInput input;
    protected SerializerPojo serializer;


    public ElsaObjectInputStream(InputStream stream) throws IOException {
        this(new DataInputStream(stream), new SerializerPojo());
    }

    public ElsaObjectInputStream(DataInput input, SerializerPojo serializer) throws IOException {
        super();
        this.input = input;
        this.serializer = serializer;
    }

    @Override
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return serializer.deserialize(input, -1);
    }

    @Override
    public void close() throws IOException {
        if(input instanceof Closeable)
            ((Closeable)input).close();
    }
}
