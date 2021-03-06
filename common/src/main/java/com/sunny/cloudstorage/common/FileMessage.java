package com.sunny.cloudstorage.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;
    private int messageNumber;

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public void setMessageNumber(int messageNumber) {
        this.messageNumber = messageNumber;
    }

    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    public FileMessage(String filename, byte[] data, int messageNumber) {
        this.filename = filename;
        this.data = data;
        this.messageNumber = messageNumber;
    }

//    @Override
//    protected void deallocate() {
//        this.data = null;
//        this.filename = null;
//    }
//
//    @Override
//    public ReferenceCounted touch(Object hint) {
//        return this;
//    }
}
