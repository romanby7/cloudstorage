package com.sunny.cloudstorage.common;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;


public class Utils {

    public byte[] readBytes(RandomAccessFile raf, long offset, long length) throws IOException {

        byte[] byteBuf;
        if (length - offset < (long) Constants.FRAME_CHUNK_SIZE) {
            byteBuf = new byte[(int) (length - offset)];
        } else {
            byteBuf = new byte[Constants.FRAME_CHUNK_SIZE];
        }
        raf.seek(offset);
        raf.read(byteBuf);
        raf.close();
        return byteBuf;

    }

    public void writeBytes(long offset, byte[] data, Path path) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
        raf.seek(offset);
        raf.write(data);
        raf.close();
    }

    public boolean isFileChunksCompleted(RandomAccessFile raf, long offset, long length) throws IOException {
        if ( offset > length)
        {
            raf.close();
            return true;
        }
        return false;
    }

}
