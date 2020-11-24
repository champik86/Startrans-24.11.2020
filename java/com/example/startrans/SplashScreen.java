package com.example.startrans;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.startrans.util.Constants.PREFERENCES_SETTINGS;

public class SplashScreen extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 2000;

//    private FirebaseAuth mAuth;

    private SharedPreferences sharedPreferences;
    public static final String IS_USER = "is_user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        sharedPreferences = getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE);
//        mAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                FirebaseUser currentUser = mAuth.getCurrentUser();
                updateUI();  //currentUser
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void updateUI() {  //FirebaseUser account
        if (!sharedPreferences.contains(IS_USER)) {
            startActivity(new Intent(this, LogIn.class));
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}

