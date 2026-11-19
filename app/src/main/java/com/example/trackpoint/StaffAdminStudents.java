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

    StudentRepository repository;
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

        repository = new StudentRepository(this);

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

        final String selectedDept = spFilterDepartment.getSelectedItem() != null ? spFilterDepartment.getSelectedItem().toString() : "All Departments";
        final String selectedYear = spFilterYear.getSelectedItem() != null ? spFilterYear.getSelectedItem().toString() : "All Years";
        final String query = searchStudent.getText().toString().trim().toLowerCase();

        TrackPointRepository cloudRepo = TrackPointRepository.getInstance(this);
        cloudRepo.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> dbList) {
                studentListContainer.removeAllViews();

                if (dbList == null || dbList.isEmpty()) {
                    TextView emptyView = new TextView(StaffAdminStudents.this);
                    if (filterByAdminName != null && !filterByAdminName.isEmpty()) {
                        emptyView.setText("No students assigned to " + filterByAdminName + " in Neon database");
                    } else {
                        emptyView.setText("No students found in Neon database");
                    }
                    emptyView.setPadding(16, 32, 16, 32);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setTextColor(Color.parseColor("#68738A"));
                    studentListContainer.addView(emptyView);
                    return;
                }

                int count = 0;
                for (StudentDto student : dbList) {
                    if (student.getName() == null) continue;

                    if (filterByAdminName != null && !filterByAdminName.isEmpty()) {
                        if (student.getAssignedAdmin() != null && !student.getAssignedAdmin().equalsIgnoreCase(filterByAdminName)) {
                            continue;
                        }
                    }

                    if (!selectedDept.equals("All Departments") && student.getDepartment() != null && !student.getDepartment().equalsIgnoreCase(selectedDept)) {
                        continue;
                    }

                    if (!selectedYear.equals("All Years") && student.getYear() != null && !student.getYear().equalsIgnoreCase(selectedYear)) {
                        continue;
                    }

                    if (!query.isEmpty() && !student.getName().toLowerCase().contains(query)
                            && (student.getRegisterNumber() != null && !student.getRegisterNumber().toLowerCase().contains(query))) {
                        continue;
                    }

                    addStudentCard(student);
                    count++;
                }

                if (count == 0) {
                    TextView emptyView = new TextView(StaffAdminStudents.this);
                    emptyView.setText("No students found matching filters");
                    emptyView.setPadding(16, 32, 16, 32);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setTextColor(Color.parseColor("#68738A"));
                    studentListContainer.addView(emptyView);
                }
            }

            @Override
            public void onError(Exception e) {
                studentListContainer.removeAllViews();
                TextView emptyView = new TextView(StaffAdminStudents.this);
                emptyView.setText("Failed to load students from Neon database");
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
        String regNo = student.getRegisterNumber() != null ? student.getRegisterNumber() : "REG";
        String dept = student.getDepartment() != null ? student.getDepartment() : "CSE";
        String yr = student.getYear() != null ? student.getYear() : "III Year";
        tvId.setText(regNo + "  •  " + dept + " (" + yr + ")");
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
