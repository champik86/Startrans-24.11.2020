package com.example.startrans;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.startrans.mailSending.SendMail;
import com.example.startrans.data.User;
import com.example.startrans.data.UserDao;
import com.example.startrans.data.UserDatabase;
import com.example.startrans.data.UserOrder;
import com.example.startrans.data.UserOrderDao;
import com.example.startrans.data.UserOrderDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.startrans.util.Constants.IS_USER;
import static com.example.startrans.util.Constants.PREFERENCES_SETTINGS;

public class LogIn extends AppCompatActivity implements DialogInterface.OnClickListener {

    @BindView(R.id.etEmail)
    EditText etEmail;
    @BindView(R.id.etPassword)
    EditText etPassword;
    @BindView(R.id.btnLogin)
    Button btnLogin;
    @BindView(R.id.tvRegister)
    TextView tvRegister;
    @BindView(R.id.tvForgotPassword)
    TextView tvForgotPassword;

    public static final String TAG = "myTag";

    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    private static final String ID_VALUE = "id_value";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private UserDatabase userDB;
    private UserDao userDao;
    private UserOrderDatabase userOrderDB;
    private UserOrderDao userOrderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        ButterKnife.bind(this);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        sharedPreferences = getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE);

        userDB = Room.databaseBuilder(this, UserDatabase.class, "userDatabase")
                .allowMainThreadQueries()
                .build();
        userDao = userDB.userDao();
        userOrderDB = Room.databaseBuilder(this, UserOrderDatabase.class, "database")
                .allowMainThreadQueries()
                .build();
        userOrderDao = userOrderDB.userOrderDao();
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

    @OnClick({R.id.btnLogin, R.id.tvRegister, R.id.tvForgotPassword})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnLogin:
                signIn();
                break;
            case R.id.tvRegister:
                startActivity(new Intent(LogIn.this, Register.class));
                finish();
                break;
            case R.id.tvForgotPassword:
                forgotPassword();
                break;
        }
    }

    private void forgotPassword() {
        View view = getLayoutInflater().inflate(R.layout.forgot_password, null);
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Забыли пароль?")
                .setPositiveButton("OK", null)
                .setView(view)
                .show();
        EditText etForgotPassword = view.findViewById(R.id.etForgotPassword);
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnline()) {
                    if (etForgotPassword.getText().toString().isEmpty()) {
                        etForgotPassword.setError("Пожалуйста, введите E-MAIL");
                        etForgotPassword.requestFocus();
                        Log.d(TAG, "else: " + etForgotPassword.getText().toString());
                        return;
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(etForgotPassword.getText().toString()).matches()) {
                        etForgotPassword.setError("Неккоректный E-MAIL");
                        etForgotPassword.requestFocus();
                        Log.d(TAG, "else if: " + etForgotPassword.getText().toString());
                        return;
                    } else {
                        String email = etForgotPassword.getText().toString();
                        DocumentReference docRef = db.collection("users").document(email);
                        Log.d(TAG, "else: " + email);
                        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot documentSnapshot = task.getResult();
                                    if (documentSnapshot != null && documentSnapshot.exists()) {
                                        String forgottenPassword = documentSnapshot.getString("password");
                                        SendMail sm = new SendMail(LogIn.this, email, "Забытый пароль", "Ваш пароль: " + forgottenPassword);
                                        sm.execute();
                                        alertDialog.dismiss();
                                        Toast.makeText(LogIn.this, "Пароль отправлен на ваш e-mail", Toast.LENGTH_LONG).show();
                                        Log.d("documentSnapshot", documentSnapshot.getString("password"));
                                    } else {
                                        alertDialog.dismiss();
                                        Toast.makeText(LogIn.this, "Неверный e-mail, или вы ещё не зарегистрированы", Toast.LENGTH_LONG).show();
                                        Log.d("documentSnapshot", "No such document");
                                    }
                                } else {
                                    alertDialog.dismiss();
                                    Toast.makeText(LogIn.this, "Ошибка. Свяжитесь с представителем компании", Toast.LENGTH_LONG).show();
                                    Log.d("documentSnapshot", "get failed with ", task.getException());
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(LogIn.this, "Нет подключения к Интернету", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void signIn() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Пожалуйста, введите E-MAIL");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Неккоректный E-MAIL");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Пожалуйста, введите ПАРОЛЬ");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("ПАРОЛЬ должен состоять минимум из 6 символов");
            etPassword.requestFocus();
            return;
        }
        if (isOnline()) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(IS_USER, true);
                                editor.apply();
                                getUserFromFirestore();
                                getUserOrdersFromFirestore();
                            } else {
                                Toast.makeText(LogIn.this, "Неверные данные или вы ещё не зарегестрированы", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(ProgressBar.GONE);
                            }
                        }
                    });
        } else {
            Toast.makeText(LogIn.this, "Нет подключения к Интернету", Toast.LENGTH_LONG).show();
        }
    }

    private void getUserFromFirestore() {
        DocumentReference docRef = db.collection("users").document(etEmail.getText().toString());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                userDao.insert(user);
            }
        });
    }

    private void getUserOrdersFromFirestore() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final int[] i = new int[1];
        db.collection("users").document(userID).collection("orders").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.d(TAG, "onSuccess: List Empty");
                            progressBar.setVisibility(ProgressBar.GONE);
                            Toast.makeText(LogIn.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LogIn.this, MainActivity.class));
                            finish();
                        } else {
                            List<UserOrder> userOrders = queryDocumentSnapshots.toObjects(UserOrder.class);
                            for (UserOrder userOrder : userOrders) {
                                userOrderDao.insert(userOrder);
                                Log.d(TAG, "onSuccess: " + userOrder);
                            }
                            i[0] = userOrders.size();
                            Log.d(TAG, i[0] + "");
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(ID_VALUE, i[0] + 1);
                            editor.apply();
                            progressBar.setVisibility(ProgressBar.GONE);
                            Toast.makeText(LogIn.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LogIn.this, MainActivity.class));
                            finish();
                        }
                    }
                });

    }

    @Override
    public void onBackPressed() {
        openQuitDialog();
    }

    private void openQuitDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("ВЫХОД")
                .setMessage("Вы уверены, что хотите выйти?")
                .setIcon(R.drawable.ic_exit)
                .setCancelable(false)
                .setPositiveButton("Да", this)
                .setNegativeButton("Нет", this);
        android.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case android.app.AlertDialog.BUTTON_POSITIVE:
                finish();  //Auto-generated method stub
                System.exit(0);
                break;
            case android.app.AlertDialog.BUTTON_NEGATIVE:
                break;  //Auto-generated method stub
        }
    }

}