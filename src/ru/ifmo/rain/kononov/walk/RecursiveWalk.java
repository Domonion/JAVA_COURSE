package ru.ifmo.rain.kononov.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: RecursiveWalk [input file, output file]");
            return;
        }
        try {
            Path input = Paths.get(args[0]);
            Path output = Paths.get(args[1]);
            Path parent = output.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (
                    BufferedReader in = Files.newBufferedReader(input);
                    BufferedWriter out = Files.newBufferedWriter(output);
            ) {
                String line;
                MySimpleFileVisitor simple = new MySimpleFileVisitor(out);
                try {
                    while ((line = in.readLine()) != null) {
                        try {
                            Files.walkFileTree(Paths.get(line), simple);
                        } catch (InvalidPathException | SecurityException e) {
                            simple.writeHash(false, line);
                        } catch (IOException e) {
                            System.out.println("Cannot write to output file: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Cannot process input file: " + e.getMessage());
                }

            } catch (SecurityException e) {
                System.out.println("Cannot open file (security reason): " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Cannot open file: " + e.getMessage());
            }
        } catch (InvalidPathException e) {
            System.out.println("Error with path: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Cannot create directory for output file: " + e.getMessage());
        }
    }
}
