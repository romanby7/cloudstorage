package com.sunny.cloudstorage.common;

public class FileRequest extends AbstractMessage {
    private String filename;
    private FileCommand fileCommand;
    private FileMessage fileMessage;

    public FileMessage getFileMessage() {
        return fileMessage;
    }

    public String getFilename() {
        return filename;
    }

    public FileCommand getFileCommand() {
        return fileCommand;
    }

    public FileRequest(FileCommand fileCommand, String filename) {
        this.fileCommand = fileCommand;
        this.filename = filename;
    }

    public FileRequest(FileCommand fileCommand) {
        this.fileCommand = fileCommand;
    }

    public FileRequest(FileCommand fileCommand, FileMessage fileMessage) {
        this.fileCommand = fileCommand;
        this.fileMessage = fileMessage;
    }
}
