package com.sunny.cloudstorage.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class Utils {

    public static void processBytes(FileMessage fm, ByteArrayOutputStream baos, String pathPart) {
        baos.write(fm.getData(), 0, fm.getData().length);
        if (baos.size() >= Constants.FILE_CHUNK_SIZE) {
            String stringPath = pathPart + fm.getFilename();
            Path path = Paths.get(stringPath);
            try {
                if (!Files.exists(path)) {
                    Files.write(path, baos.toByteArray(), StandardOpenOption.CREATE_NEW);
                } else {
                    Files.write(path, baos.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            baos.reset();
        }
    }


}
