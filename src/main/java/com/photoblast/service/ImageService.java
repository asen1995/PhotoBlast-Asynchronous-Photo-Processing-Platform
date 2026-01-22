package com.photoblast.service;

/**
 * Service interface for image processing operations.
 * <p>
 * Defines the contract for processing uploaded photos including
 * resizing, watermarking, and thumbnail generation.
 * </p>
 */
public interface ImageService {

    /**
     * Resizes an image to the configured dimensions.
     *
     * @param imagePath path to the original image file
     * @param photoId   unique identifier for the photo
     */
    void resize(String imagePath, String photoId);

    /**
     * Applies a watermark to an image.
     *
     * @param imagePath path to the original image file
     * @param photoId   unique identifier for the photo
     */
    void watermark(String imagePath, String photoId);

    /**
     * Generates a thumbnail version of an image.
     *
     * @param imagePath path to the original image file
     * @param photoId   unique identifier for the photo
     */
    void thumbnail(String imagePath, String photoId);
}
