package me.redstoner2019.utils;

import com.github.luben.zstd.Zstd;
import java.io.*;
import java.nio.file.Files;

import static java.nio.file.Files.readAllBytes;

public class CompressionUtility {
    public static void compressFile(File inputFile, File outputFile, int compressionLevel) throws IOException {
        byte[] inputBytes = readAllBytes(inputFile.toPath());
        byte[] compressed = Zstd.compress(inputBytes, compressionLevel);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(compressed);
        }

        System.out.println("Compressed " + inputFile.getName() + " to " + outputFile.getName());
    }

    public static void decompressFile(File inputFile, File outputFile) throws IOException {
        byte[] compressedBytes = readAllBytes(inputFile.toPath());
        long decompressedSize = Zstd.decompressedSize(compressedBytes);
        byte[] decompressed = Zstd.decompress(compressedBytes, (int) decompressedSize);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(decompressed);
        }

        System.out.println("Decompressed " + inputFile.getName() + " to " + outputFile.getName());
    }

    public static ByteArrayInputStream decompressFileAsStream(File compressedImageFile) throws IOException {
        byte[] compressed = readAllBytes(compressedImageFile.toPath());
        long decompressedSize = Zstd.decompressedSize(compressed);
        byte[] decompressed = Zstd.decompress(compressed, (int) decompressedSize);

        return new ByteArrayInputStream(decompressed);
    }
}

