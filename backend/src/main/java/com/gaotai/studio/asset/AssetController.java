package com.gaotai.studio.asset;

import com.gaotai.studio.user.CurrentUser;
import com.gaotai.studio.user.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public AssetResponse upload(@CurrentUser User user, @RequestPart("file") MultipartFile file) {
        return assetService.store(user, file);
    }
}
