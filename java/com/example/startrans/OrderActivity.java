package com.example.startrans;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.startrans.mailSending.SendMail;
import com.example.startrans.data.UserOrderDatabase;
import com.example.startrans.data.UserDao;
import com.example.startrans.data.UserDatabase;
import com.example.startrans.data.UserOrder;
import com.example.startrans.data.UserOrderDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import static com.example.startrans.util.Constants.PREFERENCES_SETTINGS;

public class OrderActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "myTag";

    private Button btnDate, btnTime, btnSendOrder;
    private String[] arrVehicleType = {"тент", "открытая", "цельномет."};
    private Spinner spVehicleType;
    private EditText etFrom, etTo, etWeight, etSize, etAdds;
    private String typeOfVehicle;
    private int id = 1;

    private String message = "";

    private SharedPreferences sharedPreferences;
    private static final String ID_VALUE = "id_value";


    private UserOrderDao userOrderDao;
    private UserDao userDao;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        initData();
        setOnClickListener();
        initAdapterVehicleType();

        if (sharedPreferences.contains(ID_VALUE)) {
            id = sharedPreferences.getInt(ID_VALUE, 0);
        }
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void initData() {
        etFrom = findViewById(R.id.etFrom);
        etTo = findViewById(R.id.etTo);
        etWeight = findViewById(R.id.etWeight);
        etSize = findViewById(R.id.etSize);
        etAdds = findViewById(R.id.etAdds);
        spVehicleType = findViewById(R.id.spVehicleType);

        btnDate = findViewById(R.id.btnDate);

        btnTime = findViewById(R.id.btnTime);


        btnSendOrder = findViewById(R.id.btnSendOrder);

        UserOrderDatabase userOrderDB = Room.databaseBuilder(this, UserOrderDatabase.class, "database")
                .allowMainThreadQueries()
                .build();
        userOrderDao = userOrderDB.userOrderDao();
        UserDatabase userDB = Room.databaseBuilder(this, UserDatabase.class, "userDatabase")
                .allowMainThreadQueries()
                .build();
        userDao = userDB.userDao();

        sharedPreferences = getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE);
    }

    private void setOnClickListener() {
        btnDate.setOnClickListener(this);
        btnTime.setOnClickListener(this);
        btnSendOrder.setOnClickListener(this);
    }

    private void initAdapterVehicleType() {
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, arrVehicleType);
        spVehicleType.setAdapter(adapter);
        spVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                typeOfVehicle = spVehicleType.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        Calendar calendar = Calendar.getInstance();
        switch (v.getId()) {
            case R.id.btnDate:
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                btnDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                            }
                        }, calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
                break;
            case R.id.btnTime:
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d", hourOfDay, minute);
                                btnTime.setText(time);
                            }
                        }, calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true);
                timePickerDialog.show();
                break;
            case R.id.btnSendOrder:
                if (isOnline()) {
                    sendOrder();
                } else {
                    Toast.makeText(OrderActivity.this, "Нет подключения к Интернету", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void sendOrder() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (btnDate.getText().toString().isEmpty()) {
            btnDate.setError("Введите дату");
            btnDate.requestFocus();
            return;
        }
        if (btnTime.getText().toString().isEmpty()) {
            btnTime.setError("Введите время");
            btnTime.requestFocus();
            return;
        }
        if (etFrom.getText().toString().length() < 2) {
            etFrom.setError("Введите место загрузки");
            etFrom.requestFocus();
            return;
        }
        if (etTo.getText().toString().length() < 2) {
            etTo.setError("Введите место выгрузки");
            etTo.requestFocus();
            return;
        }
        if (etWeight.getText().toString().isEmpty()) {
            etWeight.setError("Введите массу");
            etWeight.requestFocus();
            return;
        }
        double weight = Double.parseDouble(etWeight.getText().toString());
        if (weight < 1 || weight > 22) {
            etWeight.setError("Введите массу от 1 до 22");
            etWeight.requestFocus();
            return;
        }
        if (etSize.getText().toString() != null) {
            double size = Double.parseDouble(etSize.getText().toString());
            if (size < 1 || size > 120) {
                etSize.setError("Введите объем от 1 до 120");
                etSize.requestFocus();
                return;
            }
        }

        String name = userDao.getById(0).getName();  //get info from firestore and put in SQLite
        String phone = userDao.getById(0).getPhone();

        message = "id: " + id + ". " +
                "Место загрузки: " + etFrom.getText().toString() + ". " + ". Место выгрузки: " + etTo.getText().toString() + ". " +
                "Дата и время: " + btnDate.getText().toString() + ", " + btnTime.getText().toString() + ". " +
                "Тип авто: " + typeOfVehicle + ". " +
                "Масса: " + etWeight.getText().toString() + " т. " +
                "Объем: " + etSize.getText().toString() + " куб.см. " +
                "Примечания: " + etAdds.getText().toString() + ". " +
                "Имя: " + name + ". " +
                "Телефон: " + phone + ".";


        sendEmail();

        UserOrder userOrder = new UserOrder(
                id,
                "Место загрузки: " + etFrom.getText().toString() + ".",
                "Место выгрузки: " + etTo.getText().toString() + ".",
                btnDate.getText().toString() + ", " + btnTime.getText().toString(),
                typeOfVehicle,
                etWeight.getText().toString() + " т.",
                etSize.getText().toString() + " куб.см",
                etAdds.getText().toString()
        );
        userOrderDao.insert(userOrder);
        db.collection("users").document(userID).collection("orders").document("user_order_item" + id)
                .set(userOrder);
        id++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(ID_VALUE, id);
        editor.apply();
        startActivity(new Intent(this, MainActivity.class));
        Toast.makeText(this, "Спасибо за заявку. В течении часа с вами свяжется представитель компании.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendEmail() {

        SendMail sm = new SendMail(this, "startransmailsender@gmail.com", "Новая заявка", message);
        sm.execute();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}