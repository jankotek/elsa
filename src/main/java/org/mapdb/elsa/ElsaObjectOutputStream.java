package org.mapdb.elsa;

import java.io.*;

/**
 *
 */
public class ElsaObjectOutputStream extends OutputStream implements ObjectOutput{

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
    public void writeObject(Object obj) throws IOException {
        serializer.serialize(out, obj);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        if(out instanceof Closeable)
            ((Closeable)out).close();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }
}
