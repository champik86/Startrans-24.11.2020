package com.example.startrans;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.startrans.data.PolylineData;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener,
        View.OnClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener {

    public static final String TAG = "MapsActivity";
    public static final String TAGdb = "dbFire";
    @BindView(R.id.etWeight)
    EditText etWeight;
    @BindView(R.id.etSize)
    EditText etSize;
    private String duration, direction, startAdress, endAdress, price, coordOrigin, coordDest;
    private double weight, size, distance;

    private Button btnFinalCount;
    private ImageButton btnClearMap;

    private GoogleMap mMap;
    private GeoApiContext mGeoApiContext = null;

    private ArrayList<LatLng> listPoints;
    private Double destLat, destLng, origLat, origLng;
    private ArrayList<PolylineData> mPolylinesData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.api_key))
                    .build();
        }
        initData();
        setOnClickListener();

        if (!isOnline()) {
            Toast.makeText(this, "Для корректной работы приложения требуется подключение к Интернету", Toast.LENGTH_LONG).show();
        }
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void initData() {
        listPoints = new ArrayList<>();
        btnFinalCount = findViewById(R.id.btnFinalCount);
        btnClearMap = findViewById(R.id.btnClearMap);
    }

    private void setOnClickListener() {
        btnFinalCount.setOnClickListener(this);
        btnClearMap.setOnClickListener(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnPolylineClickListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        LatLng kiev = new LatLng(50.4547, 30.5238);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(kiev));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                // reset markers when already 2
                if (listPoints.size() == 2) {
                    listPoints.clear();
                    mMap.clear();
                }
                //save first point
                listPoints.add(latLng);
                //create marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if (listPoints.size() == 1) {
                    //add first marker to the map
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else {
                    //add second marker
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);
                if (listPoints.size() == 2) {
                    getRequestUrl(listPoints.get(0), listPoints.get(1));
                    calculateDirections();
                }
            }
        });
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        // Toast.makeText(this, "Ваши координаты:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    public void getRequestUrl(LatLng origin, LatLng dest) {
        origLat = origin.latitude;
        origLng = origin.longitude;
        destLat = dest.latitude;
        destLng = dest.longitude;
    }

    private void calculateDirections() {
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                destLat,
                destLng
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(origLat, origLng)

        );
        com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(
                origLat, origLng
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].legs[0].startAddress);
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].legs[0].endAddress);
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());

            }
        });
        coordOrigin = origin.toString();
        coordDest = destination.toString();
    }

    private void addPolylinesToMap(final DirectionsResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (mPolylinesData.size() > 0) {
                    for (PolylineData polylineData : mPolylinesData) {
                        polylineData.getPolyline().remove();
                    }
                    mPolylinesData.clear();
                    mPolylinesData = new ArrayList<>();
                }
                long distance = 1999999999;
                for (DirectionsRoute route : result.routes) {
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(MapsActivity.this, R.color.polylineInactive));
                    polyline.setClickable(true);
                    mPolylinesData.add(new PolylineData(polyline, route.legs[0]));

                    long tempDistance = route.legs[0].distance.inMeters;
                    if (tempDistance < distance) {
                        distance = tempDistance;
                        onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                    }
                }
            }
        });
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        for (PolylineData polylineData : mPolylinesData) {

            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if (polyline.getId().equals(polylineData.getPolyline().getId())) {
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
                polylineData.getPolyline().setZIndex(1);

                distance = polylineData.getLeg().distance.inMeters;

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Конечный пункт")
                        .snippet("Расстояние: " + polylineData.getLeg().distance));
                marker.showInfoWindow();
            } else {
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.polylineInactive));
                polylineData.getPolyline().setZIndex(0);
            }
            startAdress = polylineData.getLeg().startAddress;
            endAdress = polylineData.getLeg().endAddress;
            duration = polylineData.getLeg().duration.toString();
        }
        Log.d("MapsActivity1", "distance:  " + distance);
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 400;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    @Override
    public void onClick(View v) {
        Calendar calendar = Calendar.getInstance();
        switch (v.getId()) {
            case R.id.btnClearMap:
                resetMap();
                break;
            case R.id.btnFinalCount:
                count();
                break;
        }
    }

    private void resetMap() {
        listPoints.clear();
        mPolylinesData.clear();
        mPolylinesData = new ArrayList<>();
        mMap.clear();
    }

    private void count() {

        if (etWeight.getText().toString().isEmpty()) {
            etWeight.setError("Введите массу");
            etWeight.requestFocus();
            return;
        }
        weight = Double.parseDouble(etWeight.getText().toString());
        if (!(weight > 0 && weight <= 22)) {
            etWeight.setError("Введите массу от 0.01 до 22");
            etWeight.requestFocus();
            return;
        }
        if (etSize.getText().toString().isEmpty()) {
            etSize.setError("Введите объем");
            etSize.requestFocus();
            return;
        }
        size = Double.parseDouble(etSize.getText().toString());
        if (!(size > 0 && size <= 120)) {
            etSize.setError("Введите объем от 0.01 до 120");
            etSize.requestFocus();
            return;
        }
        if (isOnline()) {
            if (distance > 0) {
                int dist = (int) Math.round(distance) / 1000;
                Log.d("MapsActivity1", "double:  " + dist);
                // 2 tones car
                if (weight > 0 && weight <= 2 && size > 0 && size <= 20) {
                    price = dist * 11 + " грн.";
                }
                // 3 tones car
                if (weight > 2 && weight <= 3 && size > 0 && size <= 20) {
                    price = dist * 12 + " грн.";
                }
                // 5 tones car
                if (weight > 3 && weight <= 5 && size > 0 && size <= 40) {
                    price = dist * 16 + " грн.";
                }
                if (weight > 0 && weight <= 3 && size > 20 && size <= 40) {
                    price = dist * 16 + " грн.";
                }
                // 10 tones car
                if (weight > 5 && weight <= 10 && size > 0 && size <= 60) {
                    price = dist * 20 + " грн.";
                }
                if (weight > 0 && weight <= 5 && size > 40 && size <= 60) {
                    price = dist * 20 + " грн.";
                }
                // 22 tones car
                if (weight > 10 && weight <= 22 && size > 0 && size <= 120) {
                    price = dist * 25 + " грн.";
                }
                if (weight > 0 && weight <= 10 && size > 60 && size <= 120) {
                    price = dist * 25 + " грн.";
                }
                openResultDialog();
            } else {
                Toast.makeText(this, "Пожалуйста, выберите место Загрузки и Выгрузки на карте", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            Toast.makeText(MapsActivity.this, "Нет подключения к Интернету", Toast.LENGTH_LONG).show();
        }
    }

    private void openResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("СТОИМОСТЬ ПОЕЗДКИ: ~" + price)
                .setMessage("Тариф действителен только для поездок по Украине. Для Киева и области смотрите прайслист. " +
                        "\n" + "Для более точного просчета обращайтесь к специалисту компании.")
                .setPositiveButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}