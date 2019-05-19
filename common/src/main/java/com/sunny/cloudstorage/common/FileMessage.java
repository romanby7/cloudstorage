package com.sunny.cloudstorage.common;

public class FileMessage extends AbstractMessage {

    private FileCommand fileCommand;
    private String filename;
    private byte[] data;
    private long offset;
    private Object auxData;

    public FileCommand getFileCommand() {
        return fileCommand;
    }

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

    public FileMessage(FileCommand fileCommand, String filename, byte[] data, long offset) {
        this.fileCommand = fileCommand;
        this.filename = filename;
        this.data = data;
        this.offset = offset;
    }

    public FileMessage(FileCommand fileCommand, String filename, long offset) {
        this.fileCommand = fileCommand;
        this.filename = filename;
        this.offset = offset;
    }

    public FileMessage(FileCommand fileCommand, Object auxData) {
        this.fileCommand = fileCommand;
        this.auxData = auxData;
    }

    public FileMessage(FileCommand fileCommand) {
        this.fileCommand = fileCommand;
    }

}
