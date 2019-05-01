package com.sunny.cloudstorage.common;

public enum FileCommand {
    LIST_FILES,
    DOWNLOAD,
    SEND,
    SEND_PARTIAL_DATA,
    SEND_FILE_CHUNK,
    FILE_CHUNK_COMPLETED,
    DELETE
}
