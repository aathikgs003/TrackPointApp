package com.example.trackpoint;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.ArrayList;
import java.util.List;

public class StaffAdminStudents extends AppCompatActivity {

    ImageView btnBack;
    TextView txtHeaderTitle;
    Spinner spFilterDepartment, spFilterYear;
    EditText searchStudent;
    LinearLayout studentListContainer;
    Button btnAddStudent;

    TrackPointRepository repository;
    int purple = Color.parseColor("#4935B8");

    String filterByAdminName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_staff_admin_students);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        spFilterDepartment = findViewById(R.id.spFilterDepartment);
        spFilterYear = findViewById(R.id.spFilterYear);
        searchStudent = findViewById(R.id.searchStudent);
        studentListContainer = findViewById(R.id.studentListContainer);
        btnAddStudent = findViewById(R.id.btnAddStudent);

        filterByAdminName = getIntent().getStringExtra("adminName");
        if (filterByAdminName != null && !filterByAdminName.isEmpty()) {
            txtHeaderTitle.setText("Students (" + filterByAdminName + ")");
        } else {
            txtHeaderTitle.setText("Students");
        }

        btnBack.setOnClickListener(v -> finish());

        // Setup Filter Spinners
        setupSpinner(spFilterDepartment, new String[]{"All Departments", "CSE", "AI & DS", "ECE", "IT", "MECH"});
        setupSpinner(spFilterYear, new String[]{"All Years", "I Year", "II Year", "III Year", "IV Year"});

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spFilterDepartment.setOnItemSelectedListener(filterListener);
        spFilterYear.setOnItemSelectedListener(filterListener);

        searchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(StaffAdminStudents.this, StaffAddStudentActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFilters();
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void applyFilters() {
        if (studentListContainer == null) return;

        String selectedDept = spFilterDepartment.getSelectedItem() != null ? spFilterDepartment.getSelectedItem().toString() : "All Departments";
        String selectedYear = spFilterYear.getSelectedItem() != null ? spFilterYear.getSelectedItem().toString() : "All Years";
        String query = searchStudent.getText().toString().trim().toLowerCase();

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> rawList) {
                studentListContainer.removeAllViews();

                List<StudentDto> filteredList = new ArrayList<>();
                if (rawList != null) {
                    for (StudentDto s : rawList) {
                        boolean matchesSearch = query.isEmpty() ||
                                (s.getName() != null && s.getName().toLowerCase().contains(query)) ||
                                (s.getRegisterNumber() != null && s.getRegisterNumber().toLowerCase().contains(query));

                        boolean matchesDept = "All Departments".equalsIgnoreCase(selectedDept) ||
                                (s.getDepartment() != null && (s.getDepartment().equalsIgnoreCase(selectedDept) || s.getDepartment().toLowerCase().contains(selectedDept.toLowerCase())));

                        boolean matchesYear = "All Years".equalsIgnoreCase(selectedYear) ||
                                (s.getYear() != null && s.getYear().equalsIgnoreCase(selectedYear));

                        boolean matchesAdmin = filterByAdminName == null || filterByAdminName.isEmpty() ||
                                (s.getAssignedAdmin() != null && s.getAssignedAdmin().equalsIgnoreCase(filterByAdminName));

                        if (matchesSearch && matchesDept && matchesYear && matchesAdmin) {
                            filteredList.add(s);
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    TextView emptyView = new TextView(StaffAdminStudents.this);
                    if (filterByAdminName != null && !filterByAdminName.isEmpty()) {
                        emptyView.setText("No students assigned to " + filterByAdminName + " in database");
                    } else {
                        emptyView.setText("No students found in database");
                    }
                    emptyView.setPadding(16, 32, 16, 32);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setTextColor(Color.parseColor("#68738A"));
                    studentListContainer.addView(emptyView);
                    return;
                }

                for (StudentDto student : filteredList) {
                    addStudentCard(student);
                }
            }

            @Override
            public void onError(Exception e) {
                studentListContainer.removeAllViews();
                TextView emptyView = new TextView(StaffAdminStudents.this);
                emptyView.setText("Failed to load students from database");
                emptyView.setPadding(16, 32, 16, 32);
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setTextColor(Color.parseColor("#68738A"));
                studentListContainer.addView(emptyView);
            }
        });
    }

    private void addStudentCard(StudentDto student) {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        card.setBackgroundResource(R.drawable.action_card);
        card.setElevation(dpToPx(2));

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
        avatar.setLayoutParams(avatarParams);
        avatar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        avatar.setImageResource(R.drawable.profile_symbol);
        avatar.setColorFilter(purple);
        avatar.setBackgroundResource(R.drawable.card_purple);

        LinearLayout infoCol = new LinearLayout(this);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoCol.setLayoutParams(infoParams);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setPadding(dpToPx(12), 0, 0, 0);

        TextView tvName = new TextView(this);
        tvName.setText(student.getName());
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextSize(16);
        tvName.setTextColor(Color.parseColor("#18233F"));

        TextView tvId = new TextView(this);
        tvId.setText(student.getRegisterNumber() + "  •  " + student.getDepartment() + " (" + student.getYear() + ")");
        tvId.setTextSize(12);
        tvId.setTextColor(Color.parseColor("#68738A"));

        TextView tvAdmin = new TextView(this);
        String assignedName = student.getAssignedAdmin() != null ? student.getAssignedAdmin() : "Aathi";
        tvAdmin.setText("Admin: " + assignedName);
        tvAdmin.setTextSize(12);
        tvAdmin.setTextColor(purple);

        infoCol.addView(tvName);
        infoCol.addView(tvId);
        infoCol.addView(tvAdmin);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(26);
        arrow.setTextColor(Color.parseColor("#68738A"));

        card.addView(avatar);
        card.addView(infoCol);
        card.addView(arrow);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(StaffAdminStudents.this, StaffStudentDetails.class);
            intent.putExtra("studentName", student.getName());
            intent.putExtra("studentId", student.getRegisterNumber());
            intent.putExtra("department", student.getDepartment());
            intent.putExtra("year", student.getYear());
            intent.putExtra("phone", student.getPhone());
            intent.putExtra("email", student.getEmail());
            intent.putExtra("attendance", "95%");
            intent.putExtra("progress", "90%");
            intent.putExtra("tasks", "1");
            startActivity(intent);
        });

        studentListContainer.addView(card);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
