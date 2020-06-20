package com.throwntech.groceryfinder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    int PERMISSION_ID = 56;
    SearchView searchView;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private double latitude, longitude;
    private int proximityRadius = 8000;

    List<String> branchLocation = Arrays.asList("New Delhi", "Delhi", "new delhi", "New delhi", "new Delhi", "delhi", "mumbai", "Mumbai", "Chennai", "chennai", "Bangalore", "bangalore", "Bengaluru", "bengaluru");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        searchView = findViewById(R.id.sv_location);
        searchView.setQueryHint("Enter City");

        searchView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                searchView.setIconified(false);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                String grocery = "supermarket";
                List<Address> addressList = null;
                Object transferData[] = new Object[2];
                GetNearbyPlaces getNearbyPlaces = new GetNearbyPlaces();

                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);

                        if (addressList != null) {
                            if (!branchLocation.contains(location)) {
                                Toast.makeText(getApplicationContext(), "Service not available in this city", Toast.LENGTH_SHORT).show();
                            }
                            if (branchLocation.contains(location)) {
                                Address address = addressList.get(0);
                                mMap.clear();
                                latitude = address.getLatitude();
                                longitude = address.getLongitude();
                                String url = getURL(latitude, longitude, grocery);
                                transferData[0] = mMap;
                                transferData[1] = url;

                                getNearbyPlaces.execute(transferData);
                            }
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Invalid Address", Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        mapFragment.getMapAsync(this);
    }

    private String getURL(double latitude, double longitude, String nearby) {

        StringBuilder googleURL = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googleURL.append("location=" + latitude + "," + longitude);
        googleURL.append("&radius=" + proximityRadius);
        googleURL.append("&type=" + nearby);
        googleURL.append("&key=" + "AIzaSyDtIWXQDUA1ufc_Vff3qbz522DnZ26Nk9w");

        Log.d("MapsActivity", "url = " + googleURL.toString());

        return googleURL.toString();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLastLocation();
        buildGoogleApiClient();
    }

    private void getLastLocation() {
        if(checkPermissions()) {
            if(isLocationEnabled()) {
                getDeviceLocation();
            }
            else {
                float zoomLevel = 5.0f;
                LatLng IndiaCenter = new LatLng(23.1815, 79.9864);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(IndiaCenter, zoomLevel));
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }
        else {
            LatLng IndiaCenter = new LatLng(23.1815, 79.9864);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(IndiaCenter, 5));
            requestPermissions();
        }
    }

    private boolean checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE},
                PERMISSION_ID
        );
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the current device location");
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {

                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: got the location");
                        Location currentLocation = (Location) task.getResult();
                        assert currentLocation != null;
                        final LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        float zoom = 15f;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                        mMap.setMyLocationEnabled(true);
                    }
                    else {
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(MapsActivity.this, "Couldn't find the current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
                buildGoogleApiClient();
            }
        }
    }

    /*@Override
    public void onResume(){
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }*/
}