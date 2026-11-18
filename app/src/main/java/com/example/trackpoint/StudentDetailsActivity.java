package com.example.trackpoint;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

public class StudentDetailsActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView tvStudentName, tvRollNumber, tvDepartment, tvYear, tvEmail, tvPhone;
    Button btnEditStudent, btnDeleteStudent;

    TrackPointRepository repository;
    StudentDto currentStudent;
    String rollNoParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvRollNumber = findViewById(R.id.tvRollNumber);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvYear = findViewById(R.id.tvYear);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);

        btnEditStudent = findViewById(R.id.btnEditStudent);
        btnDeleteStudent = findViewById(R.id.btnDeleteStudent);

        btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("student_roll_no")) {
                rollNoParam = intent.getStringExtra("student_roll_no");
            } else if (intent.hasExtra("student_reg_no")) {
                rollNoParam = intent.getStringExtra("student_reg_no");
            }
        }

        if (btnEditStudent != null) {
            btnEditStudent.setOnClickListener(v -> openEditStudentScreen());
        }

        if (btnDeleteStudent != null) {
            btnDeleteStudent.setOnClickListener(v -> showDeleteConfirmationDialog());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentDetails();
    }

    private void loadStudentDetails() {
        if (rollNoParam == null || rollNoParam.isEmpty()) return;

        // Fetch live student row from Neon PostgreSQL
        repository.getStudentByRegNo(rollNoParam, new TrackPointRepository.RepositoryCallback<StudentDto>() {
            @Override
            public void onSuccess(StudentDto s) {
                if (s != null) {
                    currentStudent = s;
                    if (tvStudentName != null) tvStudentName.setText(s.getName());
                    if (tvRollNumber != null) tvRollNumber.setText(s.getRegisterNumber());
                    if (tvDepartment != null) tvDepartment.setText(s.getDepartment());
                    if (tvYear != null) tvYear.setText(s.getYear());
                    if (tvEmail != null) tvEmail.setText(s.getEmail());
                    if (tvPhone != null) tvPhone.setText(s.getPhone());
                }
            }

            @Override
            public void onError(Exception e) {
                // Fallback to intent values if network delay occurs
                Intent intent = getIntent();
                if (intent != null) {
                    String name = intent.getStringExtra("student_name");
                    String rollNo = intent.getStringExtra("student_roll_no");
                    if (rollNo == null) rollNo = intent.getStringExtra("student_reg_no");
                    String dept = intent.getStringExtra("student_dept");
                    String year = intent.getStringExtra("student_year");
                    String email = intent.getStringExtra("student_email");
                    String phone = intent.getStringExtra("student_phone");

                    if (name != null) {
                        currentStudent = new StudentDto(
                                name,
                                rollNo != null ? rollNo : rollNoParam,
                                dept != null ? dept : "CSE",
                                year != null ? year : "III Year",
                                email != null ? email : "",
                                phone != null ? phone : "",
                                "123",
                                "Aathi"
                        );

                        if (tvStudentName != null) tvStudentName.setText(currentStudent.getName());
                        if (tvRollNumber != null) tvRollNumber.setText(currentStudent.getRegisterNumber());
                        if (tvDepartment != null) tvDepartment.setText(currentStudent.getDepartment());
                        if (tvYear != null) tvYear.setText(currentStudent.getYear());
                        if (tvEmail != null) tvEmail.setText(currentStudent.getEmail());
                        if (tvPhone != null) tvPhone.setText(currentStudent.getPhone());
                    }
                }
            }
        });
    }

    private void openEditStudentScreen() {
        if (currentStudent == null) {
            Toast.makeText(this, "No student record loaded to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent editIntent = new Intent(StudentDetailsActivity.this, AddStudentActivity.class);
        editIntent.putExtra("is_edit_mode", true);
        editIntent.putExtra("student_roll_no", currentStudent.getRegisterNumber());
        startActivity(editIntent);
    }

    private void showDeleteConfirmationDialog() {
        if (currentStudent == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete " + currentStudent.getName() + " (" + currentStudent.getRegisterNumber() + ") from Neon database?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    repository.deleteStudent(currentStudent.getRegisterNumber(), new TrackPointRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(StudentDetailsActivity.this, "Student " + currentStudent.getName() + " deleted from Neon database!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(StudentDetailsActivity.this, "Failed to delete student from Neon: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
