package com.example.startrans;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.startrans.data.User;
import com.example.startrans.data.UserDao;
import com.example.startrans.data.UserDatabase;
import com.example.startrans.data.UserOrderAdapter;
import com.example.startrans.data.UserOrderDatabase;
import com.example.startrans.data.UserOrder;
import com.example.startrans.data.UserOrderDao;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.startrans.util.Constants.ERROR_DIALOG_REQUEST;
import static com.example.startrans.util.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.startrans.util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.example.startrans.util.Constants.PREFERENCES_SETTINGS;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        DialogInterface.OnClickListener {

    private static final String TAG = "MainActivity";

    private TextView tvEmail;
    private String number = "";
    private FloatingActionButton floatingActionButton;
    public Boolean mLocationPermissionGranted = false;

    private ArrayList <UserOrder> orderList = new ArrayList<>();
    private UserOrderDao userOrderDao;
    private UserOrderDatabase userOrderDB;

    private UserDatabase userDB;
    private UserDao userDao;

    private RecyclerView rvOrders;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(this);

        sharedPreferences = getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE);

        userOrderDB = Room.databaseBuilder(this, UserOrderDatabase.class, "database")
                .allowMainThreadQueries()
                .build();
        userOrderDao = userOrderDB.userOrderDao();
        userDB = Room.databaseBuilder(this, UserDatabase.class, "userDatabase")
                .allowMainThreadQueries()
                .build();
        userDao = userDB.userDao();

        List<UserOrder> userOrders = userOrderDao.getAll();
        for (UserOrder userOrder : userOrders) {
            orderList.add(userOrder);
        }
            Collections.reverse(orderList);

        if (!isOnline()) {
            Toast.makeText(this, "Для корректной работы приложения требуется подключение к Интернету", Toast.LENGTH_LONG).show();
        }
            rvOrders = findViewById(R.id.rvOrders);
            adapter = new UserOrderAdapter(orderList, this);
            layoutManager = new LinearLayoutManager(this);
            rvOrders.setAdapter(adapter);
            rvOrders.setLayoutManager(layoutManager);
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // PERMISSIONS: googleServices are installed?, GPS permissions.

    private boolean checkMapServices() {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true;
            }
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Для работы приложения нужны GPS данные, включить их?")
                .setCancelable(false)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "GoogleServices не подключены", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {

                } else {
                    getLocationPermission();
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {

            } else {
                getLocationPermission();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.countThePrice:
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
                finish();
                return true;
            case R.id.info:
                View view = getLayoutInflater().inflate(R.layout.info, null);
                tvEmail = view.findViewById(R.id.tvEmail);
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Контактная информация");
                alertDialog.setIcon(R.drawable.contacts);
                alertDialog.setCancelable(true);
                alertDialog.setView(view);
                alertDialog.show();
                return true;
            case R.id.priceList:
                startActivity(new Intent(MainActivity.this, PriceList.class));
                return true;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.commit();
                User user = userDao.getById(0);
                if (userDao.getById(0) != null) {
                userDao.delete(user);}
                List<UserOrder> userOrders = userOrderDao.getAll();
                for (UserOrder userOrder : userOrders) {
                    userOrderDao.delete(userOrder);
                }
                startActivity(new Intent(MainActivity.this, LogIn.class));
                Toast.makeText(MainActivity.this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void call() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
        startActivity(callIntent);
    }

    public void call1(View view) {
        number = "tel:0973739667";
        call();
    }

    public void call2(View view) {
        number = "tel:0994459946";
        call();
    }

    public void facebook(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/STARTRANS.TRUCKS"));
        startActivity(browserIntent);
    }

    public void copyEmail(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("test", tvEmail.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "E-MAIL скопирован", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatingActionButton:
                Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        openQuitDialog();
    }

    private void openQuitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ВЫХОД")
                .setMessage("Вы уверены, что хотите выйти?")
                .setIcon(R.drawable.ic_exit)
                .setCancelable(false)
                .setPositiveButton("Да", this)
                .setNegativeButton("Нет", this);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                finish();  //Auto-generated method stub
                System.exit(0);
                break;
            case AlertDialog.BUTTON_NEGATIVE:
                break;  //Auto-generated method stub
        }
    }

}