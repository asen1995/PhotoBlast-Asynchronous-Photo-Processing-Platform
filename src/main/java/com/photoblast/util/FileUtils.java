package com.photoblast.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.isNull;

/**
 * Utility class for common file operations.
 */
@UtilityClass
public class FileUtils {

    /**
     * Extracts the file extension from the given filename or path.
     *
     * @param filename the filename or path
     * @return the file extension including the dot, or ".jpg" as default
     */
    public String getExtension(String filename) {
        if (isNull(filename)) {
            return ".jpg";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".jpg";
    }

    /**
     * Ensures the specified directory exists, creating it if necessary.
     *
     * @param directory the directory path
     * @return the Path object for the directory
     * @throws IOException if the directory cannot be created
     */
    public Path ensureDirectoryExists(String directory) throws IOException {
        Path path = Paths.get(directory);
        Files.createDirectories(path);
        return path;
    }
}
