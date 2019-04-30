package com.sunny.cloudstorage.common;

public final class Constants {

    private Constants() {
    }

    public static final int FRAME_SIZE = 1024 * 1024 * 50;
    public static final int FRAME_CHUNK_SIZE = FRAME_SIZE / 500;
    public static final int FILE_CHUNK_SIZE = FRAME_SIZE * 2;
}
