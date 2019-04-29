package com.sunny.cloudstorage.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class Utils {

    public static void processBytes(FileMessage fm, String pathPart) {
        Path path = Paths.get(pathPart + fm.getFilename());
        byte[] data = fm.getData();

        System.out.println(pathPart + path.getFileName() + ": " + fm.getMessageNumber());

        try {
            if (fm.getMessageNumber() == 1) {
                Files.write(path, data, StandardOpenOption.CREATE_NEW);
            } else {
                Files.write(path, data, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

}
