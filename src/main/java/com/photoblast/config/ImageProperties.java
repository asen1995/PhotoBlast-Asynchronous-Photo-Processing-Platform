package com.photoblast.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for image processing operations.
 * <p>
 * Maps properties from application.yml under the {@code photoblast} prefix.
 * </p>
 */
@Getter
@Configuration
public class ImageProperties {

    @Value("${photoblast.storage.processed-dir}")
    private String processedDir;

    @Value("${photoblast.storage.thumbnail-dir}")
    private String thumbnailDir;

    @Value("${photoblast.image.resize.width}")
    private int resizeWidth;

    @Value("${photoblast.image.resize.height}")
    private int resizeHeight;

    @Value("${photoblast.image.thumbnail.width}")
    private int thumbnailWidth;

    @Value("${photoblast.image.thumbnail.height}")
    private int thumbnailHeight;

    @Value("${photoblast.image.watermark.path}")
    private String watermarkPath;

    @Value("${photoblast.image.watermark.opacity}")
    private float watermarkOpacity;
}
