package com.example.slider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MapFragment extends Fragment {

    MapView mapView;
    private GoogleMap googleMap;
    private DatabaseReference databaseReference;
    LocationManager locationManager;
    LocationListener locationListener;
    Switch swGPS;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String UserID;
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final int MIN_TIME = 3000;
    private final int MIN_DISTANCE = 5;
    Marker marker;
    ArrayList<LatLng> directionPoints = new ArrayList<>();
    PolylineOptions rectLine;
    Polyline routePolyline;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        swGPS = view.findViewById(R.id.swGPS);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        UserID = firebaseAuth.getCurrentUser().getUid();

        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume();

        swGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestLocation();
            }
        });

        mapView.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                databaseReference = FirebaseDatabase.getInstance().getReference().child("Locations").child(UserID);
                locationManager=(LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

                locationListener=new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if(location != null) {
                            if(swGPS.isChecked()) {
                                showToast(R.drawable.ic_map, "GPS updated.");
                                databaseReference.setValue(location);
                                googleMap.clear();
                                LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                                directionPoints.add(position);

                                firebaseFirestore.collection(UserID)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                                        marker = googleMap.addMarker(new MarkerOptions().position(position).title(documentSnapshot.getString("Username")).snippet(documentSnapshot.getString("Email")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                                        CameraPosition cameraPosition = new CameraPosition.Builder().target(position).zoom(18).build();
                                                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                        rectLine = new PolylineOptions().width(15).color(R.color.green_2);
                                                        for (int i = 0; i < directionPoints.size(); i++) {
                                                            rectLine.add(directionPoints.get(i));
                                                        }
                                                        if (routePolyline != null) {
                                                            routePolyline.remove();
                                                        }
                                                        routePolyline = googleMap.addPolyline(rectLine);
                                                    }
                                                }
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("Map", e.getMessage());
                                    }
                                });
                            }
                        }
                    }
                };

                requestLocation();

                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                Location location = snapshot.getValue(Location.class);
                                if(location != null){
                                    marker.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
                                }
                            } catch (Exception e) {
                                Log.d("Map", e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        firebaseFirestore.collection(UserID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                if (documentSnapshot.getString("Role").equals("Admin")) {
                                    swGPS.setVisibility(View.INVISIBLE);
                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Locations");
                                    reference.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            googleMap.clear();
                                            for (DataSnapshot datasnapshot : snapshot.getChildren()) {
                                                String UserID = datasnapshot.child(datasnapshot.getKey()).getKey();
                                                Map<String, Object> location = (Map<String, Object>) datasnapshot.getValue();
                                                LatLng position = new LatLng((Double) location.get("latitude"), (Double) location.get("longitude"));
                                                firebaseFirestore.collection(UserID)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if(task.isSuccessful()){
                                                                    for (DocumentSnapshot documentSnapshot: task.getResult()){
                                                                        googleMap.addMarker(new MarkerOptions().position(position).title(documentSnapshot.getString("Username")).snippet(documentSnapshot.getString("Email")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                                                    }
                                                                }
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.d("Settings",e.getMessage());
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Admin", e.getMessage());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void requestLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            showToast(R.drawable.ic_map, "The app requires location permission to be granted.");
            return;
        } else {
            if(locationManager != null){
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if(swGPS.isChecked()){
                        showToast(R.drawable.ic_map, "GPS Tracking activated successfully.");
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME,MIN_DISTANCE,locationListener);
                    }
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    if(swGPS.isChecked()) {
                        showToast(R.drawable.ic_map, "GPS Tracking activated successfully.");
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
                    }
                } else {
                    showToast(R.drawable.ic_warning, "GPS disabled.");
                }
            }
        }
    }

    public void showToast(int icon, String text) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) getView().findViewById(R.id.toast_root));
        Toast toast = new Toast(getContext());
        toast.setGravity(Gravity.TOP, 0, 0);
        ((ImageView) layout.findViewById(R.id.toast_image)).setImageDrawable(getResources().getDrawable(icon));
        ((TextView) layout.findViewById(R.id.toast_text)).setText(text);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        if(requestCode==1)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
                {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            }
        }
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(getContext(), perms)) {
            return;
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
}