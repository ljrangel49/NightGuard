package com.example.nightguard;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.maps.MapView;

import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.viewport.ViewportPlugin;
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions;
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing;
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions;
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState;
import com.mapbox.maps.plugin.viewport.transition.ViewportTransition;

import com.mapbox.maps.plugin.gestures.GesturesPlugin;


import java.util.List;



public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private PermissionsManager permissionsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

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
        mapView.getMapboxMap().loadStyle(Style.LIGHT,
                style -> {
            setUpLocationComponent();
            setUpViewportTracking();

            GesturesPlugin gesturesPlugin = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
                    if (gesturesPlugin != null) {
                        gesturesPlugin.addOnMapClickListener(point -> {
                            double latitude = point.latitude();
                            double longitude = point.longitude();

                            // Launch SubmitReportActivity with lat/lng
                            Intent intent = new Intent(MainActivity.this, SubmitReportActivity.class);
                            intent.putExtra("latitude", latitude);
                            intent.putExtra("longitude", longitude);
                            startActivity(intent);

                            return true;
                        });
                    }
        });
    }

    private void setUpLocationComponent() {
        LocationComponentPlugin locationComponentPlugin =
                mapView.getPlugin(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID);
        if (locationComponentPlugin != null) {
            // Enable plugin to show user location
            locationComponentPlugin.setEnabled(true);

            // Show default puck
            //locationComponentPlugin.setLocationPuck(new LocationPuck2D());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to PermissionsManager
        if (permissionsManager != null) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setUpViewportTracking() {
        ViewportPlugin viewportPlugin = mapView.getPlugin(Plugin.MAPBOX_VIEWPORT_PLUGIN_ID);

        if (viewportPlugin !=null)
        {
            FollowPuckViewportState followPuckViewportState = viewportPlugin.makeFollowPuckViewportState(new FollowPuckViewportStateOptions.Builder().bearing(new FollowPuckViewportStateBearing.Constant(0.0)).build());

            ViewportTransition transition =
                    viewportPlugin.makeDefaultViewportTransition(
                            new DefaultViewportTransitionOptions.Builder().maxDurationMs(1).build()
                    );

            viewportPlugin.transitionTo(followPuckViewportState, transition, success -> {
                if (success) {
                    // Camera following user location
                    Toast.makeText(
                            this,
                            "Camera tracking location.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

        }

    }
}