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
import java.io.File;
import java.io.FileOutputStream;
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
    /** Log tag for debugging */
    private static final String TAG = "UploadImageActivity";

    /** Request code for camera permission */
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    /** Request code for selecting an image from the gallery */
    public static final int SELECT_IMAGE = 100;

    /** Request code for capturing an image from the camera */
    private static final int CAPTURE_IMAGE = 101;

    private Button btnUseCamera, btnUploadImage, btnConfirm, btnCancel, btnBack;
    private ImageView ivPreview;

    private Uri pendingUri = null;  // Uri reference for the selected image from the gallery
    private Bitmap pendingBitmap = null; // Bitmap reference for the captured image from the camera
    private String pendingExtensionType = null; // type extension for the selected image (gallery or camera)

    private StorageReference storageRef;  // Firebase Storage reference for uploading images

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
            if (!ConnectivityReceiver.isNetworkAvailable(UploadImageActivity.this)){
                String localPath = saveImageLocally();
                if (localPath != null){
                    Toast.makeText(this, "Image saved locally!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("localImagePath", localPath);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save image locally!", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (pendingUri != null) {
                    // We have a gallery Uri
                    uploadImageToFirebase(pendingUri, pendingExtensionType);
                } else if (pendingBitmap != null) {
                    // We have a camera bitmap
                    uploadByteArrayToFirebase(pendingBitmap);
                } else {
                    Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cancel -> reset everything so user can pick again
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

    /**
     * Checks for camera permission and requests it if not granted;
     * otherwise, launches the camera.
     */
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

    /**
     * Launches the device camera to capture an image.
     * <p>
     * If no camera is available, shows a Toast message.
     */
    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            startActivityForResult(cameraIntent, CAPTURE_IMAGE);
        } else {
            Toast.makeText(this, "No camera detected on this device", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the gallery so the user can pick an image from local storage.
     */
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
            if (!ImageUtils.allowedExtensionType(extensionType)) {
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
            if (fileSize > ImageUtils.MAX_FILE_SIZE) {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(is);
                Bitmap compressedBitmap = ImageUtils.compressBitmap(originalBitmap);
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
    /**
     * Validates and if needed compresses a camera-captured bitmap if it exceeds the size limit.
     *
     * @param photo The captured bitmap from the camera
     */
    private void validateCameraBitmap(Bitmap photo) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        if (stream.toByteArray().length > ImageUtils.MAX_FILE_SIZE){
            Bitmap compressedBitmap = ImageUtils.compressBitmap(photo);
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

    /**
     * Uploads a gallery-selected image file to Firebase Storage.
     * <p>
     * Upon successful upload, returns the download URL to the calling activity.
     *
     * @param uri            The URI of the gallery image
     * @param extensionType  The extension type of the image
     */
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

    /**
     * Uploads a camera-captured bitmap to Firebase Storage.
     * <p>
     * Converts the bitmap to a byte array and sends it to Firebase. Returns the
     * download URL to the calling activity upon success.
     *
     * @param photoBitmap The camera-captured bitmap to upload
     */
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

    /**
     * Receives camera permission results and launches camera if granted.
     *
     * @param requestCode  The request code passed in requestPermissions
     * @param permissions  The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
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

    public String saveImageLocally() {
        try {
            // Determine which image we have: either from camera (bitmap) or from gallery (uri)
            Bitmap bitmapToSave = null;
            if (pendingBitmap != null) {
                bitmapToSave = pendingBitmap;
            } else if (pendingUri != null) {
                InputStream inputStream = getContentResolver().openInputStream(pendingUri);
                bitmapToSave = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }

            if (bitmapToSave == null) {
                return null;
            }

            // Create a unique file name and file in the app's internal storage directory
            String fileName = "offline_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);

            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            // Return the absolute file path for later use
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}