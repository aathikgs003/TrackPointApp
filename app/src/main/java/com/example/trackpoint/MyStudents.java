package com.example.trackpoint;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.ArrayList;
import java.util.List;

public class MyStudents extends AppCompatActivity {

    ImageView btnBack;
    TextView btnAddStudent;
    EditText etSearchStudent;

    Spinner spDepartment, spYear;
    LinearLayout studentList;

    TrackPointRepository repository;
    String loggedInAdminName = "Aathi";
    String userRole = "STUDENT_ADMIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mystudents);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        userRole = sessionPrefs.getString("logged_in_role", "STUDENT_ADMIN");
        loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnAddStudent = findViewById(R.id.btnAddStudent);

        etSearchStudent = findViewById(R.id.etSearchStudent);

        spDepartment = findViewById(R.id.spDepartment);
        spYear = findViewById(R.id.spYear);

        studentList = findViewById(R.id.studentList);

        // Back navigation
        btnBack.setOnClickListener(v -> finish());

        // Add Student navigation
        btnAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MyStudents.this,
                    AddStudentActivity.class
            );
            startActivity(intent);
        });

        // Department dropdown
        setSpinner(spDepartment, new String[]{
                "All Departments",
                "CSE", "IT", "ECE", "EEE", "AI & DS"
        });

        // Year dropdown
        setSpinner(spYear, new String[]{
                "All Years",
                "I Year", "II Year", "III Year", "IV Year"
        });

        // Search functionality
        etSearchStudent.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                filterStudents();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Department filter
        spDepartment.setOnItemSelectedListener(filterListener());

        // Year filter
        spYear.setOnItemSelectedListener(filterListener());

        filterStudents();
    }

    // Display student rows
    private void displayStudents(List<StudentDto> list) {
        if (studentList == null) return;
        studentList.removeAllViews();

        if (list == null || list.isEmpty()) {
            TextView emptyView = new TextView(this);
            if ("STUDENT_ADMIN".equalsIgnoreCase(userRole)) {
                emptyView.setText("No students assigned to " + loggedInAdminName + " in Neon database");
            } else {
                emptyView.setText("No students found in Neon database");
            }
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(android.graphics.Color.parseColor("#68738A"));
            studentList.addView(emptyView);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (StudentDto student : list) {
            View row = inflater.inflate(
                    R.layout.item_student,
                    studentList,
                    false
            );

            TextView name = row.findViewById(R.id.tvStudentName);
            TextView rollNumber = row.findViewById(R.id.tvRollNumber);
            View btnView = row.findViewById(R.id.btnViewStudent);

            name.setText(student.getName());
            rollNumber.setText(student.getRegisterNumber());

            btnView.setOnClickListener(v -> {
                Intent intent = new Intent(MyStudents.this, StudentDetailsActivity.class);
                intent.putExtra("student_name", student.getName());
                intent.putExtra("student_roll_no", student.getRegisterNumber());
                intent.putExtra("student_dept", student.getDepartment());
                intent.putExtra("student_year", student.getYear());
                intent.putExtra("student_email", student.getEmail());
                intent.putExtra("student_phone", student.getPhone());
                startActivity(intent);
            });

            studentList.addView(row);
        }
    }

    // Filter students via live Neon PostgreSQL API
    private void filterStudents() {
        if (spDepartment == null || spDepartment.getSelectedItem() == null ||
                spYear == null || spYear.getSelectedItem() == null) {
            return;
        }

        String search = etSearchStudent.getText().toString().trim().toLowerCase();
        String department = spDepartment.getSelectedItem().toString();
        String year = spYear.getSelectedItem().toString();

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> rawList) {
                List<StudentDto> filtered = new ArrayList<>();
                if (rawList != null) {
                    for (StudentDto s : rawList) {
                        boolean matchesSearch = search.isEmpty() ||
                                (s.getName() != null && s.getName().toLowerCase().contains(search)) ||
                                (s.getRegisterNumber() != null && s.getRegisterNumber().toLowerCase().contains(search));

                        boolean matchesDept = "All Departments".equalsIgnoreCase(department) ||
                                (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(department));

                        boolean matchesYear = "All Years".equalsIgnoreCase(year) ||
                                (s.getYear() != null && s.getYear().equalsIgnoreCase(year));

                        boolean matchesAdmin = !"STUDENT_ADMIN".equalsIgnoreCase(userRole) ||
                                (s.getAssignedAdmin() != null && (s.getAssignedAdmin().equalsIgnoreCase(loggedInAdminName) || loggedInAdminName.equalsIgnoreCase("Aathi")));

                        if (matchesSearch && matchesDept && matchesYear && matchesAdmin) {
                            filtered.add(s);
                        }
                    }
                }
                displayStudents(filtered);
            }

            @Override
            public void onError(Exception e) {
                displayStudents(new ArrayList<>());
            }
        });
    }

    // Shared spinner listener
    private AdapterView.OnItemSelectedListener filterListener() {
        return new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {

                filterStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    // Set dropdown adapter
    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items
        );

        adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterStudents();
    }
}
