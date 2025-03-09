package com.example.feelink;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;



/**
 * Handles image uploads with compression and validation for mood event photographs
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 02.02.01.01 - Photo upload UI implementation</li>
 *   <li>US 02.02.01.02 - Photo validation and processing</li>
 *   <li>US 02.02.01.03 - Image URL storage integration</li>
 *   <li>US 02.03.01.01 - File size validation</li>
 *   <li>US 02.03.01.02 - Image compression/resizing implementation</li>
 * </ul>
 * @see AddMoodEventActivity
 */
public class UploadImageActivity extends AppCompatActivity {
    private static final String TAG = "UploadImageActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int SELECT_IMAGE = 100;
    private static final int CAPTURE_IMAGE = 101;

    private static final int MAX_FILE_SIZE = 65536; // 65 KB
    private static final String[] ALLOWED_EXTENSIONS = {
            "image/jpeg", "image/png", "image/jpg"};

    private Button btnUseCamera, btnUploadImage, btnConfirm, btnCancel, btnBack;
    private ImageView ivPreview;

    private Uri pendingUri = null;  // for gallery images
    private Bitmap pendingBitmap = null; // for camera images
    private String pendingExtensionType = null; // store the extension type for the gallery image

    private StorageReference storageRef;

    /**
     * Initializes image upload UI and Firebase Storage references
     *
     * <p>Configures:
     * <ol>
     *   <li>Storage bucket reference for user mood images</li>
     *   <li>Camera/gallery selection buttons</li>
     *   <li>Image preview and confirmation controls</li>
     * </ol>
     *
     * @param savedInstanceState Persisted state data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        // Initialize Firebase Storage reference
        storageRef = FirebaseStorage.getInstance().getReference().child("user-mood-images");

        // initialize views
        btnUseCamera = findViewById(R.id.btnUseCamera);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnConfirm = findViewById(R.id.btnConfirmUpload);
        btnCancel = findViewById(R.id.btnCancelUpload);
        btnBack = findViewById(R.id.btnUploadImageBack);
        ivPreview = findViewById(R.id.ivPreview);


        btnUseCamera.setOnClickListener(v -> openCamera());

        btnUploadImage.setOnClickListener(v -> openGallery());

        // Confirm -> actually upload
        btnConfirm.setOnClickListener(v -> {
            if (pendingUri != null) {
                // We have a gallery Uri
                uploadImageToFirebase(pendingUri, pendingExtensionType);
            } else if (pendingBitmap != null) {
                // We have a camera bitmap
                uploadByteArrayToFirebase(pendingBitmap);
            } else {
                Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show();
            }
        });


        btnCancel.setOnClickListener(v -> {
            pendingUri = null;
            pendingBitmap = null;
            pendingExtensionType = null;
            ivPreview.setImageDrawable(null);
            ivPreview.setVisibility(android.view.View.GONE);
            btnConfirm.setVisibility(android.view.View.GONE);
            btnCancel.setVisibility(android.view.View.GONE);
        });

        //Back add mood activity without returning an url
        btnBack.setOnClickListener(v -> {
            finish();
        });

    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            startActivityForResult(cameraIntent, CAPTURE_IMAGE);
        } else {
            Toast.makeText(this, "No camera detected on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE);
    }

    /**
     * Handles image selection results from camera/gallery intents
     *
     * <p>Routes processing based on source:
     * <ul>
     *   <li>Camera images: Directly processes bitmap data</li>
     * </ul>
     *
     * @param requestCode Originating request identifier
     * @param resultCode Operation result status
     * @param data Image data payload
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            Toast.makeText(this, "No data or canceled. " + requestCode, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case SELECT_IMAGE:
                Uri galleryUri = data.getData();
                if (galleryUri != null) {
                    validateGalleryImage(galleryUri);
                }
                break;

            case CAPTURE_IMAGE:
                Bitmap photo = (Bitmap) (data.getExtras() != null ? data.getExtras().get("data") : null);
                if (photo != null) {
                    validateCameraBitmap(photo);
                }
                break;
        }
    }

    /**
     * Validates gallery images against size and format constraints
     *
     * <p>Performs:
     * <ol>
     *   <li>MIME type validation (JPEG/PNG only)</li>
     *   <li>File size check (max 65KB)</li>
     *   <li>Automatic compression if needed</li>
     * </ol>
     *
     * @param uri Gallery image URI to validate
     */
    private void validateGalleryImage(Uri uri) {
        try {
            String extensionType = getContentResolver().getType(uri);
            if (!allowedExtensionType(extensionType)) {
                Toast.makeText(this, "Only JPEG, JPG, or PNG allowed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check file size by counting bytes
            int fileSize;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    Toast.makeText(this, "Cannot read image data.", Toast.LENGTH_SHORT).show();
                    return;
                }
                fileSize = is.available();
            }
            if (fileSize > MAX_FILE_SIZE) {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(is);
                Bitmap compressedBitmap = compressBitmap(originalBitmap);
                pendingBitmap = compressedBitmap;
                pendingUri = null;
                pendingExtensionType = "image/jpeg";
                ivPreview.setVisibility(View.VISIBLE);
                ivPreview.setImageBitmap(compressedBitmap);
            } else{
                // Image is small enough
                pendingUri = uri;
                pendingBitmap = null;
                pendingExtensionType = extensionType;
                ivPreview.setVisibility(View.VISIBLE);
                ivPreview.setImageURI(uri);
            }

            // Show confirm/cancel
            btnConfirm.setVisibility(android.view.View.VISIBLE);
            btnCancel.setVisibility(android.view.View.VISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "validateGalleryImage: " + e.getMessage(), e);
            Toast.makeText(this, "Error validating gallery image.", Toast.LENGTH_SHORT).show();
        }
    }

    //Adapted with the help of:
    //https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
    private void validateCameraBitmap(Bitmap photo) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        if (stream.toByteArray().length > MAX_FILE_SIZE){
            Bitmap compressedBitmap = compressBitmap(photo);
            pendingBitmap = compressedBitmap;
            pendingUri = null;
            pendingExtensionType = "image/jpeg";
            ivPreview.setVisibility(View.VISIBLE);
            ivPreview.setImageBitmap(compressedBitmap);
        } else {
            // Good -> store
            pendingBitmap = photo;
            pendingUri = null;
            pendingExtensionType = "image/jpeg";
            ivPreview.setVisibility(View.VISIBLE);
            ivPreview.setImageBitmap(photo);
        }

        // Show confirm/cancel
        btnConfirm.setVisibility(android.view.View.VISIBLE);
        btnCancel.setVisibility(android.view.View.VISIBLE);
    }

    // Method to upload uri photo from gallery to firestore
    private void uploadImageToFirebase(Uri uri, String extensionType) {
        String extension = ".jpg";
        if ("image/png".equalsIgnoreCase(extensionType)) {
            extension = ".png";
        }
        String fileName = "img_" + System.currentTimeMillis() + extension;
        Log.d("UploadImage", "Uploading to: " + fileName);

        StorageReference fileRef = storageRef.child(fileName);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(extensionType)
                .build();

        fileRef.putFile(uri, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("imageUrl", downloadUri.toString());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Method to upload camera bitmap to firestore
    private void uploadByteArrayToFirebase(Bitmap photoBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();

        String fileName = "img_camera_" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        fileRef.putBytes(data, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("imageUrl", downloadUri.toString());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private boolean allowedExtensionType(String extensionType) {
        if (extensionType == null) return false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (extensionType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private Bitmap compressBitmap(Bitmap bitmap){
        int quality = 100;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] data  = stream.toByteArray();

        // If quality was good enough, return the original bitmap.
        if (data.length <= UploadImageActivity.MAX_FILE_SIZE) {
            return bitmap;
        }

        //Reducing quality until it fits or gets too low
        while (data.length > UploadImageActivity.MAX_FILE_SIZE && quality > 10){
            stream.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            data = stream.toByteArray();
        }

        //if quality loss didn't help, rescale the dimensions
        ImageDimensions dims = calculateNewDimensionsForBitmap(bitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, dims.width, dims.height, true);

        // reset quality and compress the now scaled bitmap.
        quality = 100;
        stream.reset();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        data = stream.toByteArray();


        while (data.length > UploadImageActivity.MAX_FILE_SIZE && quality > 10) {
            stream.reset();
            quality -= 5;
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            data = stream.toByteArray();
        }

        return scaledBitmap;
    }
    //Adapted from answer by Jason Evans
    //https://codereview.stackexchange.com/questions/70908/resizing-image-but-keeping-aspect-ratio
    /**
     * Calculates new dimensions for the bitmap based on a target "box" width.
     *
     * @param original the original Bitmap
     * @return an ImageDimensions object with new width and height values.
     */
    private ImageDimensions calculateNewDimensionsForBitmap(Bitmap original) {
        final int BOX_WIDTH = 100;
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Calculate the aspect ratio.
        float aspect = (float) originalWidth / originalHeight;
        int newWidth = (int) (BOX_WIDTH * aspect);
        int newHeight = (int) (newWidth / aspect);

        // If one dimension exceeds the BOX_WIDTH, adjust the dimensions.
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

    // camera permission for personal device testing
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Simple class for image dimensions.
     */
    private class ImageDimensions {
        int width;
        int height;

        ImageDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}