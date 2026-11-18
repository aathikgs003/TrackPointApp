package com.example.trackpoint;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

public class AddStudentActivity extends AppCompatActivity {

    TextView tvHeaderTitle;
    EditText etStudentName, etRegisterNumber, etStudentEmail, etStudentPhone;
    Spinner spDepartment, spYear;
    Button btnSaveStudent;
    ImageView btnBack;
    TrackPointRepository repository;

    boolean isEditMode = false;
    String rollNoToEdit = null;

    String[] deptItems = new String[]{"Select Department", "CSE", "ECE", "IT", "AI & DS"};
    String[] yearItems = new String[]{"Select Year", "I Year", "II Year", "III Year", "IV Year"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        repository = TrackPointRepository.getInstance(this);

        etStudentName = findViewById(R.id.etStudentName);
        etRegisterNumber = findViewById(R.id.etRegisterNumber);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPhone = findViewById(R.id.etStudentPhone);

        spDepartment = findViewById(R.id.spDepartment);
        spYear = findViewById(R.id.spYear);

        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        btnBack = findViewById(R.id.btnBack);

        setSpinner(spDepartment, deptItems);
        setSpinner(spYear, yearItems);

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("is_edit_mode", false)) {
            isEditMode = true;
            rollNoToEdit = intent.getStringExtra("student_roll_no");
            if (rollNoToEdit == null) {
                rollNoToEdit = intent.getStringExtra("student_reg_no");
            }
        }

        if (isEditMode && rollNoToEdit != null) {
            repository.getStudentByRegNo(rollNoToEdit, new TrackPointRepository.RepositoryCallback<StudentDto>() {
                @Override
                public void onSuccess(StudentDto s) {
                    if (s != null) {
                        if (etStudentName != null) etStudentName.setText(s.getName());
                        if (etRegisterNumber != null) {
                            etRegisterNumber.setText(s.getRegisterNumber());
                            etRegisterNumber.setEnabled(false);
                        }
                        if (etStudentEmail != null) etStudentEmail.setText(s.getEmail());
                        if (etStudentPhone != null) etStudentPhone.setText(s.getPhone());

                        setSpinnerSelection(spDepartment, deptItems, s.getDepartment());
                        setSpinnerSelection(spYear, yearItems, s.getYear());
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Ignored
                }
            });

            if (btnSaveStudent != null) {
                btnSaveStudent.setText("Update Student");
            }
        }

        btnBack.setOnClickListener(v -> finish());

        btnSaveStudent.setOnClickListener(v -> {
            String name = etStudentName.getText().toString().trim();
            String registerNumber = etRegisterNumber.getText().toString().trim();
            String email = etStudentEmail != null ? etStudentEmail.getText().toString().trim() : "";
            String phone = etStudentPhone != null ? etStudentPhone.getText().toString().trim() : "";

            String department = spDepartment.getSelectedItem() != null ? spDepartment.getSelectedItem().toString() : "";
            String year = spYear.getSelectedItem() != null ? spYear.getSelectedItem().toString() : "";

            if (name.isEmpty() || registerNumber.isEmpty() || email.isEmpty() || phone.isEmpty()
                    || spDepartment.getSelectedItemPosition() == 0
                    || spYear.getSelectedItemPosition() == 0) {

                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etStudentEmail.setError("Enter valid login email address");
                return;
            }

            SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
            String loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

            btnSaveStudent.setEnabled(false);

            StudentDto studentDto = new StudentDto(
                    name,
                    registerNumber,
                    department,
                    year,
                    email,
                    phone.startsWith("+91") ? phone : "+91 " + phone,
                    "123",
                    loggedInAdminName
            );

            if (isEditMode) {
                repository.updateStudent(registerNumber, studentDto, new TrackPointRepository.RepositoryCallback<StudentDto>() {
                    @Override
                    public void onSuccess(StudentDto result) {
                        btnSaveStudent.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Student " + name + " updated successfully on Neon database!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSaveStudent.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Failed to update student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else { // Add Mode
                repository.createStudent(studentDto, new TrackPointRepository.RepositoryCallback<StudentDto>() {
                    @Override
                    public void onSuccess(StudentDto result) {
                        btnSaveStudent.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Student " + name + " saved to Neon database!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSaveStudent.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Failed to save student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
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

    private void setSpinnerSelection(Spinner spinner, String[] items, String value) {
        if (spinner == null || value == null) return;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}
