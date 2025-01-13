package com.example.nightguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.nightguard.ApiClient;
import com.example.nightguard.ApiService;
import com.example.nightguard.Report;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SubmitReportActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;
    private EditText editDescription;

    private ApiService apiService;
    private Uri capturedImageUri;
    private String uploadedPhotoUrl;

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && capturedImageUri != null) {
                    Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_report);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Retrieve lat/lng
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        editDescription = findViewById(R.id.editDescription);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        apiService = ApiClient.getApiService();

        btnTakePhoto.setOnClickListener(v -> {
            launchCamera();
        });

        btnSubmit.setOnClickListener(v -> {
            submitReport();
        });
    }

    private void launchCamera() {
        // Create intent to capture an image
        File photoFile;
        try {
            photoFile = createImageFile(); // Create temp file for image
            capturedImageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.nightguard.fileprovider",
                    photoFile
            );

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
            takePhotoLauncher.launch(cameraIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void uploadPhotoToFirebase(Uri imageUri, FirebaseUploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure("Image URI is null.");
            return;
        }

        // Generate filename in Firebase Storage
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "images/" + "IMG_" + timeStamp + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoRef = storageRef.child(fileName);

        // Upload the file
        UploadTask uploadTask = photoRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    callback.onSuccess(uri.toString());
                })
        ).addOnFailureListener(e -> {
            callback.onFailure("Failed to upload photo: " + e.getMessage());
        });
    }

    private void submitReport() {
        String description = editDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedImageUri == null) {
            Toast.makeText(this, "Please capture a photo before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload the photo to Firebase before creating the report
        uploadPhotoToFirebase(capturedImageUri, new FirebaseUploadCallback() {
            @Override
            public void onSuccess(String photoUrl) {
                // Set the uploaded photo URL
                uploadedPhotoUrl = photoUrl;

                // Timestamp of report
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Create report after photo upload
                Report newReport = new Report(latitude, longitude, description, photoUrl, timestamp);

                // POST the report to Node.js server
                Call<Report> call = apiService.createReport(newReport);
                call.enqueue(new Callback<Report>() {
                    @Override
                    public void onResponse(Call<Report> call, Response<Report> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SubmitReportActivity.this,
                                    "Report submitted!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SubmitReportActivity.this,
                                    "Failed to submit report. Code: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Report> call, Throwable t) {
                        Toast.makeText(SubmitReportActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(SubmitReportActivity.this,
                        "Failed to upload photo: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface FirebaseUploadCallback {
        void onSuccess(String photoUrl);

        void onFailure(String errorMessage);
    }
}
