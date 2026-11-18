package com.example.trackpoint;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.List;

public class MyProfile extends AppCompatActivity {

    ImageView btnBack;
    Button btnLogout;
    TextView tvAdminName, tvProfileName, tvProfileEmail, tvProfileDepartment, tvProfilePhone;
    TrackPointRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        tvAdminName = findViewById(R.id.tvAdminName);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileDepartment = findViewById(R.id.tvProfileDepartment);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);

        btnBack.setOnClickListener(v -> finish());

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String loggedInAdminId = sessionPrefs.getString("logged_in_admin_id", "SADM001");
        String loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        repository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> admins) {
                if (admins != null) {
                    for (StudentAdminDto sa : admins) {
                        if (loggedInAdminId.equalsIgnoreCase(sa.getAdminId()) || loggedInAdminName.equalsIgnoreCase(sa.getName())) {
                            if (tvAdminName != null) tvAdminName.setText(sa.getName());
                            if (tvProfileName != null) tvProfileName.setText(sa.getName());
                            if (tvProfileEmail != null) tvProfileEmail.setText(sa.getEmail());
                            if (tvProfilePhone != null) tvProfilePhone.setText(sa.getPhone());
                            if (tvProfileDepartment != null) tvProfileDepartment.setText(sa.getDepartment() + " Department");
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (tvAdminName != null) tvAdminName.setText(loggedInAdminName);
                if (tvProfileName != null) tvProfileName.setText(loggedInAdminName);
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(MyProfile.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
