package com.gaotai.studio.asset;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.user.User;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetService {

    public static final long GLOBAL_MAX_BYTES = 5_242_880L;

    private final AssetMapper assetMapper;
    private final UploadStore uploadStore;
    private final TimeSource timeSource;

    public AssetService(AssetMapper assetMapper, UploadStore uploadStore, TimeSource timeSource) {
        this.assetMapper = assetMapper;
        this.uploadStore = uploadStore;
        this.timeSource = timeSource;
    }

    public AssetResponse store(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "FILE_EMPTY", "请选择一张图片");
        }
        byte[] bytes = read(file);
        if (bytes.length > GLOBAL_MAX_BYTES) {
            throw new ApiException(400, "FILE_TOO_LARGE", "文件超过 5MB 限制");
        }
        String detected = ImageSignatures.detect(bytes);
        if (detected == null) {
            throw new ApiException(400, "UNSUPPORTED_TYPE", "仅支持 JPEG 或 PNG");
        }
        String declared = ImageSignatures.normalizeDeclared(file.getContentType());
        if (declared != null && declared.startsWith("image/") && !declared.equals(detected)) {
            throw new ApiException(400, "TYPE_MISMATCH", "文件内容与类型不一致");
        }
        String extension = ImageSignatures.extension(detected);
        String id = UUID.randomUUID().toString();
        String storageName = id + extension;
        Asset asset = new Asset();
        asset.setId(id);
        asset.setUserId(user.getId());
        asset.setOriginalFilename(cleanFilename(file.getOriginalFilename()));
        asset.setMimeType(detected);
        asset.setSizeBytes(bytes.length);
        asset.setStorageName(storageName);
        asset.setCreatedAt(timeSource.now());
        uploadStore.write(storageName, bytes);
        try {
            assetMapper.insert(asset);
        } catch (RuntimeException exception) {
            uploadStore.delete(storageName);
            throw exception;
        }
        return new AssetResponse(
                asset.getId(),
                "/media/uploads/" + storageName,
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getOriginalFilename());
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ApiException(400, "FILE_UNREADABLE", "无法读取上传文件");
        }
    }

    static String cleanFilename(String name) {
        if (name == null || name.isBlank()) {
            return "image";
        }
        String trimmed = name.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        String base = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
        base = base.replace("\r", "").replace("\n", "");
        if (base.isBlank()) {
            return "image";
        }
        if (base.length() > 120) {
            return base.substring(base.length() - 120);
        }
        return base;
    }
}
