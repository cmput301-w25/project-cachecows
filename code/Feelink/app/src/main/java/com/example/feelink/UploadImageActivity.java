package com.example.feelink;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

    private void validateGalleryImage(Uri uri) {
        try {
            String extensionType = getContentResolver().getType(uri);
            if (!allowedExtensionType(extensionType)) {
                Toast.makeText(this, "Only JPEG, JPG, or PNG allowed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check file size by counting bytes
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    Toast.makeText(this, "Cannot read image data.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int fileSize = is.available();
                if (fileSize > MAX_FILE_SIZE) {
                    Toast.makeText(this, "Image cannot exceed 65 KB (yours: " + fileSize + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Passed checks
            pendingUri = uri;
            pendingBitmap = null;
            pendingExtensionType = extensionType;

            // Show preview
            ivPreview.setVisibility(android.view.View.VISIBLE);
            ivPreview.setImageURI(uri);

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
        byte[] dataBytes = stream.toByteArray();

        if (dataBytes.length > MAX_FILE_SIZE) {
            Toast.makeText(this, "Photo too large (>65 KB).", Toast.LENGTH_SHORT).show();
            return;
        }

        // Good -> store
        pendingBitmap = photo;
        pendingUri = null;
        pendingExtensionType = "image/jpeg";

        // Image preview
        ivPreview.setVisibility(android.view.View.VISIBLE);
        ivPreview.setImageBitmap(photo);

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
}