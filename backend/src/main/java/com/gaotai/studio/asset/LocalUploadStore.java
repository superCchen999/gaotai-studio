package com.gaotai.studio.asset;

import com.gaotai.studio.config.StoragePaths;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LocalUploadStore implements UploadStore {

    private final StoragePaths storagePaths;

    public LocalUploadStore(StoragePaths storagePaths) {
        this.storagePaths = storagePaths;
    }

    @Override
    public boolean exists(String storageName) {
        Path file = StoragePaths.resolveChild(storagePaths.uploads(), storageName);
        return file != null && Files.isRegularFile(file);
    }

    @Override
    public void write(String storageName, byte[] bytes) {
        Path file = StoragePaths.resolveChild(storagePaths.uploads(), storageName);
        if (file == null) {
            throw new IllegalArgumentException("非法素材文件名");
        }
        try {
            Files.createDirectories(storagePaths.uploads());
            Files.write(file, bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void delete(String storageName) {
        Path file = StoragePaths.resolveChild(storagePaths.uploads(), storageName);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 清理失败不影响已经返回给调用方的错误。
        }
    }
}
