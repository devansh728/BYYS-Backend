package com.byys.backend_otp.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

public class ImageCompressionUtil {
    private static final String UPLOAD_DIR = "uploads/profile-photos/";

    public static String compressAndSave(MultipartFile file) throws IOException {
        // Create directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);

        // Compress and save image
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);

            // Resize to max 500x500
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > 500 || height > 500) {
                float ratio = Math.min(500f / width, 500f / height);
                width = (int) (width * ratio);
                height = (int) (height * ratio);

                BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resizedImage.createGraphics();
                g.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();
                image = resizedImage;
            }

            // Compress with 70% quality
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                 ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {

                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.7f);
                writer.write(null, new IIOImage(image, null, null), param);
            }

            writer.dispose();
        }

        return UPLOAD_DIR + fileName;
    }
}
