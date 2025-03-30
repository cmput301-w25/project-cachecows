package com.example.feelink.utils;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class ImageUtils {
    /** Maximum allowed file size (65 KB) for uploaded images */
    public static final int MAX_FILE_SIZE = 65536;

    public static final String[] ALLOWED_EXTENSIONS = {
            "image/jpeg", "image/png", "image/jpg"
    };

    /**
     * Checks whether the provided extension type is in the allowed extension types.
     *
     * @param extensionType The extension type of the file
     * @return True if it's a permitted type; false otherwise
     */
    public static boolean allowedExtensionType(String extensionType) {
        if (extensionType == null) return false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (extensionType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates new dimensions for the bitmap based on a target "box" width,
     * preserving aspect ratio.
     * <p>
     * Adapted from answer by Jason Evans:
     * https://codereview.stackexchange.com/questions/70908/resizing-image-but-keeping-aspect-ratio
     *
     * @param original The original Bitmap
     * @return An ImageDimensions object with new width and height values
     */
    public static ImageDimensions calculateNewDimensionsForBitmap(Bitmap original) {
        final int BOX_WIDTH = 200;
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        float aspect = (float) originalWidth / originalHeight;
        int newWidth = (int) (BOX_WIDTH * aspect);
        int newHeight = (int) (newWidth / aspect);

        if (newWidth > BOX_WIDTH || newHeight > BOX_WIDTH) {
            if (newWidth > newHeight) {
                newWidth = BOX_WIDTH;
                newHeight = (int) (newWidth / aspect);
            } else {
                newHeight = BOX_WIDTH;
                newWidth = (int) (newHeight * aspect);
            }
        }
        return new ImageDimensions(newWidth, newHeight);
    }

    /**
     * Compresses the given bitmap to ensure it does not exceed MAX_FILE_SIZE (65 KB).
     * <p>
     * Steps:
     * <ol>
     *   <li>Iteratively reduce JPEG quality until under size or until quality is &lt;= 10</li>
     *   <li>If still too large, rescale the bitmap dimensions while preserving aspect ratio</li>
     *   <li>Re-compress after rescaling if necessary</li>
     * </ol>
     *
     * @param bitmap The original bitmap to compress
     * @return A bitmap that should be under the size limit
     */
    public static Bitmap compressBitmap(Bitmap bitmap) {
        int quality = 100;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] data  = stream.toByteArray();

        // If the image is already small enough, return it
        if (data.length <= MAX_FILE_SIZE) {
            return bitmap;
        }

        // Reduce quality until under limit
        while (data.length > MAX_FILE_SIZE && quality > 10) {
            stream.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            data = stream.toByteArray();
        }

        // If quality reduction isn't enough, rescale dimensions
        ImageDimensions dims = calculateNewDimensionsForBitmap(bitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, dims.width, dims.height, true);

        quality = 100;
        stream.reset();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        data = stream.toByteArray();

        while (data.length > MAX_FILE_SIZE && quality > 10) {
            stream.reset();
            quality -= 5;
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            data = stream.toByteArray();
        }
        return scaledBitmap;
    }


    /**
     * Simple class for image dimensions.
     */
    public static class ImageDimensions {
        public int width;
        public int height;

        public ImageDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}

