package com.sunny.cloudstorage.common;

import java.io.Serializable;

public enum FileCommand implements Serializable {
    LIST_CLIENT_FILES,
    LIST_SERVER_FILES,
    DELETE,
    SEND_FILE_CHUNK_TO_CLIENT,
    SEND_FILE_CHUNK_TO_SERVER,
    FILE_CHUNK_COMPLETED
}
