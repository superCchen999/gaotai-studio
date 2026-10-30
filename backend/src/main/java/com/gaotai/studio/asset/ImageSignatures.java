package com.gaotai.studio.asset;

public final class ImageSignatures {

    private ImageSignatures() {
    }

    public static String detect(byte[] data) {
        if (data == null || data.length < 8) {
            return null;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == (byte) 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47
                && data[4] == 0x0D
                && data[5] == 0x0A
                && data[6] == 0x1A
                && data[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }

    public static String extension(String mimeType) {
        if ("image/png".equals(mimeType)) {
            return ".png";
        }
        if ("image/jpeg".equals(mimeType)) {
            return ".jpg";
        }
        return null;
    }

    public static String normalizeDeclared(String contentType) {
        if (contentType == null) {
            return null;
        }
        String value = contentType.toLowerCase();
        int separator = value.indexOf(';');
        if (separator >= 0) {
            value = value.substring(0, separator);
        }
        value = value.trim();
        if ("image/jpg".equals(value) || "image/pjpeg".equals(value)) {
            return "image/jpeg";
        }
        if ("image/x-png".equals(value)) {
            return "image/png";
        }
        return value;
    }
}
