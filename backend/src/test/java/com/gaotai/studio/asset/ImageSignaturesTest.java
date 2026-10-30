package com.gaotai.studio.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ImageSignaturesTest {

    @Test
    void detectsPngAndJpeg() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00};
        assertEquals("image/png", ImageSignatures.detect(png));
        assertEquals("image/jpeg", ImageSignatures.detect(jpeg));
    }

    @Test
    void rejectsTextAndShortBytes() {
        assertNull(ImageSignatures.detect("hello".getBytes()));
        assertNull(ImageSignatures.detect(new byte[] {(byte) 0xFF, (byte) 0xD8}));
        assertNull(ImageSignatures.detect(null));
    }

    @Test
    void normalizesJpegAliases() {
        assertEquals("image/jpeg", ImageSignatures.normalizeDeclared("image/jpg; charset=binary"));
        assertEquals("image/png", ImageSignatures.normalizeDeclared("image/x-png"));
    }

    @Test
    void stripsPathFromFilename() {
        assertEquals("secret.png", AssetService.cleanFilename("..\\secret.png"));
        assertEquals("secret.png", AssetService.cleanFilename("../secret.png"));
        assertEquals("image", AssetService.cleanFilename("   "));
    }
}
