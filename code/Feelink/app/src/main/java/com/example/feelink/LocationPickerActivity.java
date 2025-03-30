package com.example.feelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import android.location.Geocoder;
import android.location.Address;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final boolean SKIP_AUTH_FOR_TESTING = false;
    GoogleMap mMap;
    Marker selectedMarker;
    private LatLng selectedLocation;
    private PlacesClient placesClient;
    private ListView suggestionsList;
    private ArrayAdapter<String> suggestionsAdapter;
    LatLng selectedLatLng;
    double selectedLatitude;
    double selectedLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDJTEdK53CkAlOXLDt1nmJEmZeJcX5zvW4");
        }
        placesClient = Places.createClient(this);

        // Initialize suggestions list
        suggestionsList = findViewById(R.id.suggestionsList);
        suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        suggestionsList.setAdapter(suggestionsAdapter);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                suggestionsList.setVisibility(View.GONE);
                searchForPlace(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    searchSuggestions(newText);
                } else {
                    suggestionsList.setVisibility(View.GONE);
                }
                return true;
            }
        });

        // Handle suggestion selection
        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSuggestion = suggestionsAdapter.getItem(position);
            searchView.setQuery(selectedSuggestion, false);
            suggestionsList.setVisibility(View.GONE);
            searchForPlace(selectedSuggestion);
        });

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up confirm button
        Button btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                // Get the address for the selected location
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(selectedLatLng.latitude, selectedLatLng.longitude, 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String locationName = address.getAddressLine(0);
                        selectedLatitude = selectedLatLng.latitude;
                        selectedLongitude = selectedLatLng.longitude;

                        // Create result intent with location data
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("latitude", selectedLatitude);
                        resultIntent.putExtra("longitude", selectedLongitude);
                        resultIntent.putExtra("locationName", locationName);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchSuggestions(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.newInstance(query);
        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            suggestionsAdapter.clear();
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                suggestionsAdapter.add(prediction.getFullText(null).toString());
            }
            suggestionsList.setVisibility(View.VISIBLE);
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                Log.e("LocationPicker", "Error fetching place suggestions: " + statusCode);
            }
        });
    }

    private void searchForPlace(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                if (selectedMarker != null) {
                    selectedMarker.remove();
                }
                selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(address.getAddressLine(0)));
                selectedLocation = latLng;

                // Pass the location name back to AddMoodEventActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                resultIntent.putExtra("locationName", address.getAddressLine(0));
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error searching for location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set initial camera position to Edmonton
        LatLng edmonton = new LatLng(53.5461, -113.4937);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 12));
        
        // Enable the my-location layer if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Handle map clicks
        mMap.setOnMapClickListener(latLng -> {
            // Just update the marker position without setting the location
            if (selectedMarker == null) {
                MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location");
                selectedMarker = mMap.addMarker(markerOptions);
            } else {
                selectedMarker.setPosition(latLng);
            }
            selectedLatLng = latLng;
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            selectedLatLng = currentLocation;
                            
                            // Add marker at current location
                            if (selectedMarker != null) {
                                selectedMarker.remove();
                            }
                            selectedMarker = mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location"));
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Location permission is required to show your current location",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
} 
