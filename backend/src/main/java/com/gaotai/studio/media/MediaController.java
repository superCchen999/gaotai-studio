package com.gaotai.studio.media;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.StoragePaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaController {

    private final StoragePaths storagePaths;

    public MediaController(StoragePaths storagePaths) {
        this.storagePaths = storagePaths;
    }

    @GetMapping("/media/{kind}/{name}")
    public ResponseEntity<FileSystemResource> read(@PathVariable String kind, @PathVariable String name) {
        Path directory = switch (kind) {
            case "uploads" -> storagePaths.uploads();
            case "results" -> storagePaths.results();
            default -> null;
        };
        if (directory == null) {
            throw new ApiException(404, "MEDIA_NOT_FOUND", "文件不存在");
        }
        Path file = StoragePaths.resolveChild(directory, name);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ApiException(404, "MEDIA_NOT_FOUND", "文件不存在");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType(name)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new FileSystemResource(file));
    }

    private String contentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
