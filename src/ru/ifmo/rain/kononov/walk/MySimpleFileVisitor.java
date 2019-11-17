package ru.ifmo.rain.kononov.walk;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class MySimpleFileVisitor extends SimpleFileVisitor<Path> {
    private static final int FNV_INIT = 0x811c9dc5;
    private static final int FNV_PRIME = 0x01000193;
    private static final String ERROR_HASH = String.format("%08x", 0);
    private final BufferedWriter out;

    MySimpleFileVisitor(BufferedWriter writer) {
        out = writer;
    }

    private String getHash(String filename) {
        try (FileInputStream in = new FileInputStream(filename)) {
            int hash = FNV_INIT;
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                for (int i = 0; i < read; ++i) {
                    hash = (hash * FNV_PRIME) ^ (buffer[i] & 0xff);
                }
            }
            return String.format("%08x", hash);
        } catch (IOException | SecurityException e) {
            return ERROR_HASH;
        }
    }

    void writeHash(boolean good, String filename) throws IOException {
        out.write((good ? getHash(filename) : ERROR_HASH) + " " + filename);
        out.newLine();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        writeHash(true, file.toString());
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        writeHash(false, file.toString());
        return CONTINUE;
    }

}
