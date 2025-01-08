package com.example.nightguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SubmitReportActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;
    private EditText editDescription;
    private Button btnTakePhoto;
    private Button btnSubmit;

    private ApiService apiService;
    private Uri capturedImageUri;
    private String uploadedPhotoUrl;

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && capturedImageUri != null) {

                    uploadPhotoToFirebase(capturedImageUri);
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
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSubmit = findViewById(R.id.btnSubmit);

        apiService = ApiClient.getApiService();

        btnTakePhoto.setOnClickListener(v -> {
            // Camera intent
            launchCamera();
        });

        btnSubmit.setOnClickListener(v -> {
            submitReport();
        });
    }


    private void launchCamera() {
        // Create an intent to capture an image
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // You might want to create a temporary file URI for the photo
        // We'll skip file creation details for brevity, or use MediaStore approach

        // If you want a file-based approach:
        // capturedImageUri = FileProvider.getUriForFile(...);

        // For simplicity, rely on the system to store the thumbnail:
        // NOTE: This approach only gives you a low-res thumbnail in some cases
        // A full approach requires creating a FileProvider & file path

        try {
            takePhotoLauncher.launch(cameraIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPhotoToFirebase(Uri imageUri) {
        if (imageUri == null) return;

        // Generate a unique filename in Firebase Storage
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "images/" + "IMG_" + timeStamp + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoRef = storageRef.child(fileName);

        // Upload the file
        UploadTask uploadTask = photoRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedPhotoUrl = uri.toString();
                    Toast.makeText(SubmitReportActivity.this,
                            "Photo uploaded successfully!",
                            Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            Toast.makeText(SubmitReportActivity.this,
                    "Failed to upload photo: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void submitReport() {
        String description = editDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new Report with the photoUrl from Firebase
        Report newReport = new Report(latitude, longitude, description, uploadedPhotoUrl);

        // POST to Node.js server
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
}