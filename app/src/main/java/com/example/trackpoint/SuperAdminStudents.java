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

public class SuperAdminStudents extends AppCompatActivity {

    ImageView btnBack;
    TextView txtHeaderTitle;
    EditText searchSuperStudent;
    Spinner spSuperStudentDept, spSuperStudentYear;
    LinearLayout superStudentListContainer;

    TrackPointRepository repository;
    int purple = Color.parseColor("#4935B8");
    String lockedDepartment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(purple);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_super_admin_students);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        searchSuperStudent = findViewById(R.id.searchSuperStudent);
        spSuperStudentDept = findViewById(R.id.spSuperStudentDept);
        spSuperStudentYear = findViewById(R.id.spSuperStudentYear);
        superStudentListContainer = findViewById(R.id.superStudentListContainer);

        btnBack.setOnClickListener(v -> finish());

        lockedDepartment = getIntent().getStringExtra("filterDepartment");
        if (lockedDepartment != null && !lockedDepartment.isEmpty()) {
            if (txtHeaderTitle != null) {
                txtHeaderTitle.setText(lockedDepartment + " Students");
            }
            spSuperStudentDept.setVisibility(View.GONE);
        }

        setupSpinner(spSuperStudentDept, new String[]{"All Departments", "CSE", "AI & DS", "ECE", "IT", "MECH"});
        setupSpinner(spSuperStudentYear, new String[]{"All Years", "I Year", "II Year", "III Year", "IV Year"});

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spSuperStudentDept.setOnItemSelectedListener(listener);
        spSuperStudentYear.setOnItemSelectedListener(listener);

        searchSuperStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void renderList() {
        if (superStudentListContainer == null) return;

        String dept = (lockedDepartment != null && !lockedDepartment.isEmpty()) ? lockedDepartment :
                (spSuperStudentDept.getSelectedItem() != null ? spSuperStudentDept.getSelectedItem().toString() : "All Departments");
        String year = spSuperStudentYear.getSelectedItem() != null ? spSuperStudentYear.getSelectedItem().toString() : "All Years";
        String query = searchSuperStudent.getText().toString().toLowerCase().trim();

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> rawList) {
                superStudentListContainer.removeAllViews();

                List<StudentDto> filtered = new ArrayList<>();
                if (rawList != null) {
                    for (StudentDto s : rawList) {
                        boolean matchesSearch = query.isEmpty() ||
                                (s.getName() != null && s.getName().toLowerCase().contains(query)) ||
                                (s.getRegisterNumber() != null && s.getRegisterNumber().toLowerCase().contains(query));

                        boolean matchesDept = "All Departments".equalsIgnoreCase(dept) ||
                                (s.getDepartment() != null && (s.getDepartment().equalsIgnoreCase(dept) || s.getDepartment().toLowerCase().contains(dept.toLowerCase())));

                        boolean matchesYear = "All Years".equalsIgnoreCase(year) ||
                                (s.getYear() != null && s.getYear().equalsIgnoreCase(year));

                        if (matchesSearch && matchesDept && matchesYear) {
                            filtered.add(s);
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    TextView emptyView = new TextView(SuperAdminStudents.this);
                    emptyView.setText("No students found in Neon database");
                    emptyView.setPadding(16, 32, 16, 32);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setTextColor(Color.parseColor("#68738A"));
                    superStudentListContainer.addView(emptyView);
                    return;
                }

                for (StudentDto s : filtered) {
                    LinearLayout card = new LinearLayout(SuperAdminStudents.this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, dpToPx(10));
                    card.setLayoutParams(params);
                    card.setOrientation(LinearLayout.HORIZONTAL);
                    card.setGravity(Gravity.CENTER_VERTICAL);
                    card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
                    card.setBackgroundResource(R.drawable.action_card);
                    card.setElevation(dpToPx(2));

                    ImageView avatar = new ImageView(SuperAdminStudents.this);
                    avatar.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)));
                    avatar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                    avatar.setImageResource(R.drawable.profile_symbol);
                    avatar.setColorFilter(purple);
                    avatar.setBackgroundResource(R.drawable.card_purple);

                    LinearLayout info = new LinearLayout(SuperAdminStudents.this);
                    info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(dpToPx(12), 0, 0, 0);

                    TextView tvName = new TextView(SuperAdminStudents.this);
                    tvName.setText(s.getName());
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setTextSize(16);
                    tvName.setTextColor(Color.parseColor("#18233F"));

                    TextView tvMeta = new TextView(SuperAdminStudents.this);
                    tvMeta.setText(s.getRegisterNumber() + "  •  " + s.getDepartment() + " (" + s.getYear() + ")");
                    tvMeta.setTextSize(12);
                    tvMeta.setTextColor(Color.parseColor("#68738A"));

                    TextView tvAdmin = new TextView(SuperAdminStudents.this);
                    String admin = s.getAssignedAdmin() != null ? s.getAssignedAdmin() : "Aathi";
                    tvAdmin.setText("Student Admin: " + admin);
                    tvAdmin.setTextSize(12);
                    tvAdmin.setTextColor(purple);

                    info.addView(tvName);
                    info.addView(tvMeta);
                    info.addView(tvAdmin);

                    TextView arrow = new TextView(SuperAdminStudents.this);
                    arrow.setText("›");
                    arrow.setTextSize(26);
                    arrow.setTextColor(Color.parseColor("#68738A"));

                    card.addView(avatar);
                    card.addView(info);
                    card.addView(arrow);

                    card.setOnClickListener(v -> {
                        Intent intent = new Intent(SuperAdminStudents.this, StaffStudentDetails.class);
                        intent.putExtra("studentName", s.getName());
                        intent.putExtra("studentId", s.getRegisterNumber());
                        intent.putExtra("department", s.getDepartment());
                        intent.putExtra("year", s.getYear());
                        intent.putExtra("phone", s.getPhone());
                        intent.putExtra("email", s.getEmail());
                        startActivity(intent);
                    });

                    superStudentListContainer.addView(card);
                }
            }

            @Override
            public void onError(Exception e) {
                superStudentListContainer.removeAllViews();
                TextView emptyView = new TextView(SuperAdminStudents.this);
                emptyView.setText("Failed to load students from database");
                emptyView.setPadding(16, 32, 16, 32);
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setTextColor(Color.parseColor("#68738A"));
                superStudentListContainer.addView(emptyView);
            }
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
