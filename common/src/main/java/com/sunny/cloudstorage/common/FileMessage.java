package com.sunny.cloudstorage.common;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;
    private long offset;
    private Object auxData;

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public long getOffset() {
        return offset;
    }

    public Object getAuxData() {
        return auxData;
    }

    public FileMessage(String filename, byte[] data, long offset) {
        this.filename = filename;
        this.data = data;
        this.offset = offset;
    }

    public FileMessage(String filename, long offset) {
        this.filename = filename;
        this.offset = offset;
    }

    public FileMessage(Object auxData) {
        this.auxData = auxData;
    }
}
