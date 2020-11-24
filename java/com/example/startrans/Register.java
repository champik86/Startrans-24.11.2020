package com.example.startrans;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;

import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.startrans.mailSending.SendMail;
import com.example.startrans.data.User;
import com.example.startrans.data.UserDao;
import com.example.startrans.data.UserDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import static com.example.startrans.util.Constants.IS_USER;
import static com.example.startrans.util.Constants.PREFERENCES_SETTINGS;


public class Register extends AppCompatActivity implements
        View.OnClickListener {

    private EditText etPassword, etPassword2, etName, etCompany, etEmail, etPhone, etCity;
    private String message = "";
    private ProgressBar progressBar;

    private UserDao userDao;

    private FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        initData();

    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void initData() {
        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);
        etName = findViewById(R.id.etName);
        etCompany = findViewById(R.id.etCompany);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etCity = findViewById(R.id.etCity);
        Button btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(this);

        UserDatabase userDB = Room.databaseBuilder(this, UserDatabase.class, "userDatabase")
                .allowMainThreadQueries()
                .build();
        userDao = userDB.userDao();

        sharedPreferences = getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnRegister) {
            register();
        }
    }

    private void register() {

        if (etEmail.getText().toString().isEmpty()) {
            etEmail.setError("Пожалуйста, введите E-MAIL");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches()) {
            etEmail.setError("Неккоректный E-MAIL");
            etEmail.requestFocus();
            return;
        }
        if (etPassword.getText().toString().isEmpty()) {
            etPassword.setError("Пожалуйста, введите ПАРОЛЬ");
            etPassword.requestFocus();
            return;
        }
        if (etPassword.getText().toString().length() < 6) {
            etPassword.setError("ПАРОЛЬ должен состоять минимум из 6 символов");
            etPassword.requestFocus();
            return;
        }
        if (!etPassword.getText().toString().equals(etPassword2.getText().toString())) {
            etPassword2.setError("Пароли не совпадают");
            etPassword2.requestFocus();
            return;
        }
        if (etName.getText().toString().length() < 4) {
            etName.setError("Введите имя пользователя");
            etName.requestFocus();
            return;
        }
        if (etPhone.getText().toString().isEmpty()) {
            etPhone.setError("Пожалуйста, введите номер телефона");
            etPhone.requestFocus();
            return;
        }
        if (etPhone.getText().toString().length() != 10) {
            etPhone.setError("Введите номер в следующем формате: 063 777 66 55");
            etPhone.requestFocus();
            return;
        }
        if (isOnline()) {
            progressBar.setVisibility(ProgressBar.VISIBLE);

            message = "Имя: " + etName.getText().toString() + ". " +
                    "email: " + etEmail.getText().toString() + ". " +
                    "пароль: " + etPassword.getText().toString() + ". " +
                    "телефон: " + etPhone.getText().toString() + ". " +
                    "компания: " + etCompany.getText().toString() + ". " +
                    "город: " + etCity.getText().toString();

            sendEmail();
            mAuth.createUserWithEmailAndPassword(etEmail.getText().toString(), etPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
//                        Log.d("myTag", etEmail.getText().toString());
//                        Log.d("myTag", etPassword.getText().toString());
                            if (task.isSuccessful()) {
                                addUserToFirestoreAndSQLite();
                                progressBar.setVisibility(ProgressBar.GONE);
                                Toast.makeText(Register.this, "Регистрация успешна",
                                        Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(IS_USER, true);
                                editor.apply();
                                Intent intent = new Intent(Register.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                progressBar.setVisibility(ProgressBar.GONE);
                                Toast.makeText(Register.this, "Ошибка регистрации. Возможно пользователь с таким e-mail уже существует",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(Register.this, "Нет подключения к Интернету", Toast.LENGTH_LONG).show();
        }
    }

    private void addUserToFirestoreAndSQLite () {
        //         Create a new user
        String date = Calendar.getInstance().getTime().toString();
        User user = new User(0,
                etEmail.getText().toString(),
                etPassword.getText().toString(),
                etName.getText().toString(),
                etPhone.getText().toString(),
                etCompany.getText().toString()+"",
                etCity.getText().toString()+"",
                date);
        db.collection("users").document(etEmail.getText().toString())
                .set(user);
        userDao.insert(user);
    }

    private void sendEmail() {
        //Getting content for email

        //Creating SendMail object
        SendMail sm = new SendMail(this, "startransmailsender@gmail.com", "Новый пользователь", message);

        //Executing sendmail to send email
        sm.execute();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, LogIn.class));
        finish();
    }
}