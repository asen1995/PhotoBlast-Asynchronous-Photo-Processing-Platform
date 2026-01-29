package com.photoblast.enums;

/**
 * Available processing tasks that can be performed on photos.
 */
public enum ProcessingTask {
    /** Resize the photo to configured dimensions */
    RESIZE,
    /** Apply a watermark to the photo */
    WATERMARK,
    /** Generate a thumbnail version of the photo */
    THUMBNAIL
}
