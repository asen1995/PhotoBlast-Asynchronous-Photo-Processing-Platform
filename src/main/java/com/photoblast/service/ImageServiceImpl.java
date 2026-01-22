package com.photoblast.service;

import com.photoblast.config.ImageProperties;
import com.photoblast.exception.ImageProcessingException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link ImageService} using Thumbnailator library.
 * <p>
 * Handles image manipulation operations including resizing,
 * watermarking, and thumbnail generation.
 * </p>
 */
@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    private final ImageProperties imageProperties;

    /**
     * Constructs a new ImageServiceImpl with the given configuration properties.
     *
     * @param imageProperties the image processing configuration properties
     */
    public ImageServiceImpl(ImageProperties imageProperties) {
        this.imageProperties = imageProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resize(String imagePath, String photoId) {
        log.info("Resizing image: photoId={}, path={}", photoId, imagePath);

        try {
            Path outputDir = Paths.get(imageProperties.getProcessedDir());
            Files.createDirectories(outputDir);

            String outputFilename = photoId + "_resized" + getExtension(imagePath);
            Path outputPath = outputDir.resolve(outputFilename);

            Thumbnails.of(new File(imagePath))
                    .size(imageProperties.getResizeWidth(), imageProperties.getResizeHeight())
                    .keepAspectRatio(true)
                    .toFile(outputPath.toFile());

            log.info("Resize completed: photoId={}, output={}", photoId, outputPath);

        } catch (IOException e) {
            log.error("Failed to resize image: photoId={}", photoId, e);
            throw new ImageProcessingException("Failed to resize image", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void watermark(String imagePath, String photoId) {
        log.info("Applying watermark: photoId={}, path={}", photoId, imagePath);

        try {
            Path outputDir = Paths.get(imageProperties.getProcessedDir());
            Files.createDirectories(outputDir);

            String outputFilename = photoId + "_watermarked" + getExtension(imagePath);
            Path outputPath = outputDir.resolve(outputFilename);

            File watermarkFile = new File(imageProperties.getWatermarkPath());
            if (!watermarkFile.exists()) {
                log.warn("Watermark file not found: {}. Skipping watermark.", imageProperties.getWatermarkPath());
                return;
            }

            BufferedImage watermarkImage = ImageIO.read(watermarkFile);

            Thumbnails.of(new File(imagePath))
                    .scale(1.0)
                    .watermark(Positions.BOTTOM_RIGHT, watermarkImage, imageProperties.getWatermarkOpacity())
                    .toFile(outputPath.toFile());

            log.info("Watermark applied: photoId={}, output={}", photoId, outputPath);

        } catch (IOException e) {
            log.error("Failed to apply watermark: photoId={}", photoId, e);
            throw new ImageProcessingException("Failed to apply watermark", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void thumbnail(String imagePath, String photoId) {
        log.info("Generating thumbnail: photoId={}, path={}", photoId, imagePath);

        try {
            Path outputDir = Paths.get(imageProperties.getThumbnailDir());
            Files.createDirectories(outputDir);

            String outputFilename = photoId + "_thumb" + getExtension(imagePath);
            Path outputPath = outputDir.resolve(outputFilename);

            Thumbnails.of(new File(imagePath))
                    .size(imageProperties.getThumbnailWidth(), imageProperties.getThumbnailHeight())
                    .keepAspectRatio(true)
                    .toFile(outputPath.toFile());

            log.info("Thumbnail generated: photoId={}, output={}", photoId, outputPath);

        } catch (IOException e) {
            log.error("Failed to generate thumbnail: photoId={}", photoId, e);
            throw new ImageProcessingException("Failed to generate thumbnail", e);
        }
    }

    /**
     * Extracts the file extension from the given path.
     *
     * @param path the file path
     * @return the file extension including the dot, or ".jpg" as default
     */
    private String getExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            return path.substring(lastDot);
        }
        return ".jpg";
    }
}
