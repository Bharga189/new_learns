package com.example.java12.filesmismatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Java 12 Files.mismatch()
 *
 * Files.mismatch(first, second) compares two files byte by byte and returns
 * the position of the first difference, or -1 when the contents are equal.
 *
 * Java 8-11: read both files and compare the byte arrays yourself
 * Java 12:   Files.mismatch(path1, path2)
 */
public class FilesMismatchExample {

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("java12-mismatch");
        try {
            Path first = Files.writeString(dir.resolve("first.txt"), "hello world");
            Path second = Files.writeString(dir.resolve("second.txt"), "hello java");
            Path copy = Files.writeString(dir.resolve("copy.txt"), "hello world");

            System.out.println("first vs copy: " + Files.mismatch(first, copy));     // -1
            System.out.println("first vs second: " + Files.mismatch(first, second)); // 6
        } finally {
            try (var files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // best-effort cleanup of the temporary directory
                    }
                });
            }
        }
    }
}
