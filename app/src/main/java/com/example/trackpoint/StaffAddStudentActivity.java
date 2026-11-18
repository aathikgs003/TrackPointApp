package com.example.trackpoint;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.ArrayList;
import java.util.List;

public class StaffAddStudentActivity extends AppCompatActivity {

    EditText etStudentName, etRegisterNumber, etPhone, etEmail;
    Spinner spDepartment, spYear, spAssignAdmin;
    Button btnSaveStudent;
    ImageView btnBack;

    TrackPointRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_staff_add_student);

        repository = TrackPointRepository.getInstance(this);

        etStudentName = findViewById(R.id.etStudentName);
        etRegisterNumber = findViewById(R.id.etRegisterNumber);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        spDepartment = findViewById(R.id.spDepartment);
        spYear = findViewById(R.id.spYear);
        spAssignAdmin = findViewById(R.id.spAssignAdmin);

        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        btnBack = findViewById(R.id.btnBack);

        setSpinner(spDepartment, new String[]{"Select Department", "CSE", "AI & DS", "ECE", "IT", "MECH"});
        setSpinner(spYear, new String[]{"Select Year", "I Year", "II Year", "III Year", "IV Year"});

        repository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> adminsList) {
                ArrayList<String> adminNames = new ArrayList<>();
                adminNames.add("Select Student Admin");
                if (adminsList == null || adminsList.isEmpty()) {
                    adminNames.add("Aathi");
                } else {
                    for (StudentAdminDto sa : adminsList) {
                        adminNames.add(sa.getName());
                    }
                }
                setSpinner(spAssignAdmin, adminNames.toArray(new String[0]));
            }

            @Override
            public void onError(Exception e) {
                setSpinner(spAssignAdmin, new String[]{"Select Student Admin", "Aathi"});
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnSaveStudent.setOnClickListener(v -> {
            String name = etStudentName.getText().toString().trim();
            String regNo = etRegisterNumber.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty() || regNo.isEmpty() || phone.isEmpty() || email.isEmpty()
                    || spDepartment.getSelectedItemPosition() == 0
                    || spYear.getSelectedItemPosition() == 0
                    || spAssignAdmin.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter valid email address");
                return;
            }

            String dept = spDepartment.getSelectedItem().toString();
            String year = spYear.getSelectedItem().toString();
            String assignedAdmin = spAssignAdmin.getSelectedItem().toString();

            btnSaveStudent.setEnabled(false);

            StudentDto studentDto = new StudentDto(
                    name,
                    regNo,
                    dept,
                    year,
                    email,
                    phone.startsWith("+91") ? phone : "+91 " + phone,
                    "123",
                    assignedAdmin
            );

            repository.createStudent(studentDto, new TrackPointRepository.RepositoryCallback<StudentDto>() {
                @Override
                public void onSuccess(StudentDto result) {
                    btnSaveStudent.setEnabled(true);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("studentName", name);
                    resultIntent.putExtra("studentId", regNo);
                    resultIntent.putExtra("department", dept);
                    resultIntent.putExtra("year", year);
                    resultIntent.putExtra("assignedAdmin", assignedAdmin);
                    resultIntent.putExtra("phone", phone);
                    resultIntent.putExtra("email", email);
                    setResult(RESULT_OK, resultIntent);

                    Toast.makeText(StaffAddStudentActivity.this, name + " saved to Neon database!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    btnSaveStudent.setEnabled(true);
                    Toast.makeText(StaffAddStudentActivity.this, "Failed to save student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}
