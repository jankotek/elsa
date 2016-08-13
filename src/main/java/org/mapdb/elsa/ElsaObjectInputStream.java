package org.mapdb.elsa;

import java.io.*;

/**
 * Created by jan on 4/17/16.
 */
public class ElsaObjectInputStream extends InputStream implements ObjectInput {

    protected final DataInput input;
    protected ElsaSerializerPojo serializer;


    public ElsaObjectInputStream(InputStream stream) throws IOException {
        this(new DataInputStream(stream), new ElsaSerializerPojo());
    }

    public ElsaObjectInputStream(DataInput input, ElsaSerializerPojo serializer) throws IOException {
        this.input = input;
        this.serializer = serializer;
    }

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        return serializer.deserialize(input);
    }

    @Override
    public int read() throws IOException {
        return input.readUnsignedByte();
    }

    @Override
    public void close() throws IOException {
        if(input instanceof Closeable)
            ((Closeable)input).close();
    }


    @Override
    public void readFully(byte[] b) throws IOException {
        input.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        input.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return input.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return input.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return input.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return input.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return input.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return input.readUTF();
    }
}
