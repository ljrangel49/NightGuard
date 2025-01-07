package com.example.nightguard;

import androidx.annotation.NonNull;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.maps.MapView;

import java.util.List;



public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private PermissionsManager permissionsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);

        if (PermissionsManager.areLocationPermissionsGranted(this))
        {
            initializeMap();
        }
        else
        {
            permissionsManager = new PermissionsManager(new PermissionsListener() {
                @Override
                public void onExplanationNeeded(@NonNull List<String> list) {
                    Toast.makeText(MainActivity.this, "Location permissions are necessary for use.",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPermissionResult(boolean b) {
                    if (b) // Location permissions Granted
                    {
                        initializeMap();
                    }
                    else // Location permissions denied
                    {
                        Toast.makeText(MainActivity.this, "Exiting app.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
            });

            // Request permissions
            permissionsManager.requestLocationPermissions(this);
        }

    }

    private void initializeMap() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to PermissionsManager
        if (permissionsManager != null) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}