package com.sunny.cloudstorage.common;

import java.util.List;

public class FilesListMessage extends AbstractMessage {

    private List<String> filesList;

    public List<String> getFilesList() {
        return filesList;
    }

    public FilesListMessage(List<String> filesList) {
        this.filesList = filesList;
    }
}
