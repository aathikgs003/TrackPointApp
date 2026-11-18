package com.example.trackpoint;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.LoginResponseDto;
import com.example.trackpoint.repository.TrackPointRepository;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnLogin;
    private StudentRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new StudentRepository(this);

        edtEmail = findViewById(R.id.username);
        edtPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.login);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString();

            if (email.isEmpty()) {
                edtEmail.setError("Email or ID is required");
                return;
            }

            if (password.isEmpty()) {
                edtPassword.setError("Password is required");
                return;
            }

            btnLogin.setEnabled(false);

            Log.i(TAG, "Attempting login for email: " + email);

            // Attempt Cloud Authentication via TrackPointRepository
            repository.getCloudRepository().login(email, password, "ANY", new TrackPointRepository.RepositoryCallback<LoginResponseDto>() {
                @Override
                public void onSuccess(LoginResponseDto response) {
                    btnLogin.setEnabled(true);
                    if (response != null && response.isSuccess()) {
                        Toast.makeText(MainActivity.this, "Welcome " + response.getUserName(), Toast.LENGTH_SHORT).show();
                        handleLoginSuccess(response.getRole(), response.getUserId(), response.getUserName(), response.getDepartment(), email);
                    } else {
                        performLocalFallbackAuthentication(email, password);
                    }
                }

                @Override
                public void onError(Exception e) {
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "Cloud login error: " + e.getMessage(), e);
                    performLocalFallbackAuthentication(email, password);
                }
            });
        });
    }

    private void performLocalFallbackAuthentication(String email, String password) {
        // 1. Staff Admin Role
        StaffAdminModel staffAdmin = repository.authenticateStaffAdmin(email, password);
        if (staffAdmin != null || (email.equalsIgnoreCase("suruthika@gmail.com") && password.equals("123"))) {
            String staffName = staffAdmin != null ? staffAdmin.getName() : "Suruthika J";
            String staffId = staffAdmin != null ? staffAdmin.getStaffId() : "STF001";
            String staffDept = staffAdmin != null ? staffAdmin.getDepartment() : "CSE";
            String staffEmail = staffAdmin != null ? staffAdmin.getEmail() : email;

            handleLoginSuccess("STAFF_ADMIN", staffId, staffName, staffDept, staffEmail);
            return;
        }

        // 2. Student Admin Role
        StudentAdminModel studentAdmin = repository.authenticateStudentAdmin(email, password);
        if (studentAdmin != null || (email.equalsIgnoreCase("aathi@gmail.com") && password.equals("123"))) {
            String adminId = studentAdmin != null ? studentAdmin.getAdminId() : "SADM001";
            String adminName = studentAdmin != null ? studentAdmin.getName() : "Aathi";

            handleLoginSuccess("STUDENT_ADMIN", adminId, adminName, "", email);
            return;
        }

        // 3. Student Role
        StudentModel student = repository.authenticateStudent(email, password);
        if (student != null || ((email.equalsIgnoreCase("arun@gmail.com") || email.equalsIgnoreCase("CSE22CS001")) && password.equals("123"))) {
            String regNo = student != null ? student.getRegisterNumber() : "CSE22CS001";

            handleLoginSuccess("STUDENT", regNo, student != null ? student.getName() : "Arun Kumar", "", email);
            return;
        }

        // 4. Super Admin Role
        if ((email.equalsIgnoreCase("admin@gmail.com") || email.equalsIgnoreCase("superadmin@gmail.com")) && (password.equals("123") || password.equals("admin"))) {
            handleLoginSuccess("SUPER_ADMIN", "SPADM001", "Super Admin", "", email);
            return;
        }

        Toast.makeText(MainActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
    }

    private void handleLoginSuccess(String role, String userId, String userName, String department, String email) {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sessionPrefs.edit();
        editor.putString("logged_in_role", role);

        if ("STAFF_ADMIN".equalsIgnoreCase(role)) {
            editor.putString("logged_in_staff_id", userId != null ? userId : "STF001")
                  .putString("logged_in_staff_name", userName != null ? userName : "Suruthika J")
                  .putString("logged_in_staff_dept", department != null ? department : "CSE")
                  .putString("logged_in_staff_email", email).apply();
            startActivity(new Intent(MainActivity.this, StaffAdmin.class));
        } else if ("STUDENT_ADMIN".equalsIgnoreCase(role)) {
            editor.putString("logged_in_admin_id", userId != null ? userId : "SADM001")
                  .putString("logged_in_admin_name", userName != null ? userName : "Aathi").apply();
            startActivity(new Intent(MainActivity.this, StudentAdmin.class));
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            editor.putString("logged_in_reg_no", userId != null ? userId : "CSE22CS001").apply();
            startActivity(new Intent(MainActivity.this, StudentDashboardActivity.class));
        } else if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            editor.apply();
            startActivity(new Intent(MainActivity.this, SuperAdminDashboard.class));
        }

        edtEmail.setText("");
        edtPassword.setText("");
    }
}
