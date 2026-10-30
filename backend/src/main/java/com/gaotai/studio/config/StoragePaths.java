package com.gaotai.studio.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class StoragePaths {

    private final Path root;

    public StoragePaths(StudioProperties properties) {
        this.root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void prepare() throws IOException {
        Files.createDirectories(uploads());
        Files.createDirectories(results());
    }

    public Path root() {
        return root;
    }

    public Path uploads() {
        return root.resolve("uploads");
    }

    public Path results() {
        return root.resolve("results");
    }

    public static Path resolveChild(Path directory, String name) {
        if (name == null || !name.matches("[A-Za-z0-9._-]{1,128}")) {
            return null;
        }
        Path dir = directory.toAbsolutePath().normalize();
        Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir)) {
            return null;
        }
        return file;
    }
}
