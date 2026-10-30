package com.gaotai.studio.asset;

public record AssetResponse(
        String id,
        String url,
        String mimeType,
        long sizeBytes,
        String originalFilename) {
}
