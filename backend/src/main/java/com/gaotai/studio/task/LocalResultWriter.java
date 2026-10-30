package com.gaotai.studio.task;

import com.gaotai.studio.config.StoragePaths;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class LocalResultWriter implements ResultWriter {

    private final StoragePaths storagePaths;

    public LocalResultWriter(StoragePaths storagePaths) {
        this.storagePaths = storagePaths;
    }

    @Override
    public String write(String taskId, String title) {
        if (taskId == null || !taskId.matches("[A-Za-z0-9-]{1,64}")) {
            throw new StageFailure("结果整理失败：任务编号无效");
        }
        String fileName = taskId + ".png";
        Path file = StoragePaths.resolveChild(storagePaths.results(), fileName);
        if (file == null) {
            throw new StageFailure("结果整理失败：无法确定结果路径");
        }
        try {
            Files.createDirectories(storagePaths.results());
            BufferedImage image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(new Color(0xF6, 0xF1, 0xE8));
                graphics.fillRect(0, 0, 960, 640);
                graphics.setColor(new Color(0xB8, 0x43, 0x2F));
                graphics.fillRect(0, 0, 18, 640);
                graphics.setColor(new Color(0x22, 0x1C, 0x16));
                drawLabel(graphics, title);
            } finally {
                graphics.dispose();
            }
            if (!ImageIO.write(image, "png", file.toFile())) {
                throw new StageFailure("结果整理失败：无法写入图片");
            }
        } catch (StageFailure exception) {
            throw exception;
        } catch (IOException exception) {
            throw new StageFailure("结果整理失败：无法写入图片");
        }
        return "/media/results/" + fileName;
    }

    private void drawLabel(Graphics2D graphics, String title) {
        String safe = title == null || title.isBlank() ? "制作结果" : title.trim();
        try {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
            graphics.drawString(safe, 56, 300);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            graphics.setColor(new Color(0x6B, 0x5E, 0x52));
            graphics.drawString("稿台模拟结果", 56, 352);
        } catch (RuntimeException exception) {
            graphics.setColor(new Color(0x22, 0x1C, 0x16));
            graphics.fillRect(56, 280, 280, 18);
            graphics.setColor(new Color(0xB8, 0x43, 0x2F));
            graphics.fillRect(56, 312, 160, 10);
        }
    }
}
