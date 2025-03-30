package com.example.feelink;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import com.example.feelink.utils.ImageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImageUtilsTest {

    @Test
    public void testAllowedExtensionType() {
        assertTrue(ImageUtils.allowedExtensionType("image/jpeg"));
        assertTrue(ImageUtils.allowedExtensionType("image/png"));
        assertTrue(ImageUtils.allowedExtensionType("image/jpg"));

        // Invalid file types should return false
        assertFalse(ImageUtils.allowedExtensionType("image/gif"));
    }

    @Test
    public void testCalculateNewDimensionsForBitmap() {
        //  dummy bitmap with ratio 2:1
        Bitmap dummy = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888);
        ImageUtils.ImageDimensions dims = ImageUtils.calculateNewDimensionsForBitmap(dummy);

        // ratio is preserved after calculating new dimensions
        assertEquals(200, dims.width);
        assertEquals(100, dims.height);
    }

    @Test
    public void testCompressBitmapSmallBitmapReturnsSame() {
        // a small bitmap under MAX_FILE_SIZE
        Bitmap smallBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        Bitmap resultBitmap = ImageUtils.compressBitmap(smallBitmap);
        // For small bitmaps, the original should be returned
        assertSame(smallBitmap, resultBitmap);
    }

    @Test
    public void testCompressBitmapLargeBitmap() {
        // a larger bitmap that exceeds the 65KB limit.
        Bitmap largeBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Bitmap compressedBitmap = ImageUtils.compressBitmap(largeBitmap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();

        assertTrue(data.length <= ImageUtils.MAX_FILE_SIZE);
    }
}
