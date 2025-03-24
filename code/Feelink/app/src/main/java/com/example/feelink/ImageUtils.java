package com.example.feelink;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {
    public static String saveImageLocally(Context context, Bitmap bitmapToSave) {
        try {
            String fileName = "offline_" + System.currentTimeMillis() + ".jpg";
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
