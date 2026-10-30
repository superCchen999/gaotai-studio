package com.gaotai.studio.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.support.MemoryDb;
import com.gaotai.studio.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AssetServiceTest {

    private final MemoryDb db = new MemoryDb();
    private final AssetService service = new AssetService(db.assets(), db.files(), new TimeSource());
    private final User user = user("u-linxia");

    @Test
    void storesPngAndKeepsBasename() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "dir/photo.png", "image/png", png);
        AssetResponse response = service.store(user, file);
        assertEquals("image/png", response.mimeType());
        assertEquals("photo.png", response.originalFilename());
        assertTrue(response.url().endsWith(".png"));
        assertTrue(db.fileExists(response.id() + ".png"));
    }

    @Test
    void rejectsTypeMismatchAndOversize() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x11};
        MockMultipartFile mismatch = new MockMultipartFile("file", "a.png", "image/png", jpeg);
        ApiException typeError = assertThrows(ApiException.class, () -> service.store(user, mismatch));
        assertEquals("TYPE_MISMATCH", typeError.getCode());

        byte[] huge = jpeg.clone();
        huge = new byte[(int) AssetService.GLOBAL_MAX_BYTES + 1];
        huge[0] = (byte) 0xFF;
        huge[1] = (byte) 0xD8;
        huge[2] = (byte) 0xFF;
        MockMultipartFile big = new MockMultipartFile("file", "big.jpg", "image/jpeg", huge);
        ApiException sizeError = assertThrows(ApiException.class, () -> service.store(user, big));
        assertEquals("FILE_TOO_LARGE", sizeError.getCode());
    }

    private static User user(String id) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(id);
        return user;
    }
}
