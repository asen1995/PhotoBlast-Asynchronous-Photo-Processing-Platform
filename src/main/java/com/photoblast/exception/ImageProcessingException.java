package com.photoblast.exception;

/**
 * Exception thrown when image processing operations fail.
 * <p>
 * This is a runtime exception that wraps underlying I/O or processing errors
 * that occur during image manipulation operations.
 * </p>
 */
public class ImageProcessingException extends RuntimeException {

    /**
     * Constructs a new ImageProcessingException with the specified message.
     *
     * @param message the detail message
     */
    public ImageProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new ImageProcessingException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of the exception
     */
    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
