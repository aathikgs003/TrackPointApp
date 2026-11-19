package com.example.trackpoint;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.trackpoint.network.dto.StaffAdminDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.List;

public class SuperAdminDepartmentDetails extends AppCompatActivity {

    private TextView txtDeptHeaderTitle, txtDeptFullName;
    private TextView txtDeptStaffCount, txtDeptStudentAdminCount, txtDeptStudentCount;
    private StudentRepository repository;
    private String deptCode, deptName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_super_admin_department_details);

        repository = new StudentRepository(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        txtDeptHeaderTitle = findViewById(R.id.txtDeptHeaderTitle);
        txtDeptFullName = findViewById(R.id.txtDeptFullName);
        txtDeptStaffCount = findViewById(R.id.txtDeptStaffCount);
        txtDeptStudentAdminCount = findViewById(R.id.txtDeptStudentAdminCount);
        txtDeptStudentCount = findViewById(R.id.txtDeptStudentCount);

        btnBack.setOnClickListener(v -> finish());

        deptCode = getIntent().getStringExtra("deptCode");
        deptName = getIntent().getStringExtra("deptName");

        if (deptCode != null) txtDeptHeaderTitle.setText(deptCode + " Department");
        if (deptName != null) txtDeptFullName.setText(deptName);

        loadDepartmentMetrics();

        // Open Staff Admins filtered by this Department
        findViewById(R.id.rowViewDeptStaffAdmins).setOnClickListener(v -> {
            Intent intent = new Intent(this, SuperAdminStaffAdmins.class);
            intent.putExtra("filterDepartment", deptCode);
            startActivity(intent);
        });

        // Open Student Admins filtered by this Department
        findViewById(R.id.rowViewDeptStudentAdmins).setOnClickListener(v -> {
            Intent intent = new Intent(this, SuperAdminStudentAdmins.class);
            intent.putExtra("filterDepartment", deptCode);
            startActivity(intent);
        });

        // Open Students filtered by this Department
        findViewById(R.id.rowViewDeptStudents).setOnClickListener(v -> {
            Intent intent = new Intent(this, SuperAdminStudents.class);
            intent.putExtra("filterDepartment", deptCode);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDepartmentMetrics();
    }

    private void loadDepartmentMetrics() {
        if (deptCode == null) return;

        TrackPointRepository cloudRepo = TrackPointRepository.getInstance(this);

        cloudRepo.getAllStaffAdmins(new TrackPointRepository.RepositoryCallback<List<StaffAdminDto>>() {
            @Override
            public void onSuccess(List<StaffAdminDto> list) {
                int count = 0;
                if (list != null) {
                    for (StaffAdminDto s : list) {
                        if (s.getDepartment() != null && (s.getDepartment().equalsIgnoreCase(deptCode) || s.getDepartment().toLowerCase().contains(deptCode.toLowerCase()))) {
                            count++;
                        }
                    }
                }
                if (txtDeptStaffCount != null) txtDeptStaffCount.setText(String.valueOf(count));
            }

            @Override
            public void onError(Exception e) {
                if (txtDeptStaffCount != null) txtDeptStaffCount.setText("0");
            }
        });

        cloudRepo.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> list) {
                int count = 0;
                if (list != null) {
                    for (StudentAdminDto sa : list) {
                        if (sa.getDepartment() != null && (sa.getDepartment().equalsIgnoreCase(deptCode) || sa.getDepartment().toLowerCase().contains(deptCode.toLowerCase()))) {
                            count++;
                        }
                    }
                }
                if (txtDeptStudentAdminCount != null) txtDeptStudentAdminCount.setText(String.valueOf(count));
            }

            @Override
            public void onError(Exception e) {
                if (txtDeptStudentAdminCount != null) txtDeptStudentAdminCount.setText("0");
            }
        });

        cloudRepo.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> list) {
                int count = 0;
                if (list != null) {
                    for (StudentDto st : list) {
                        if (st.getDepartment() != null && (st.getDepartment().equalsIgnoreCase(deptCode) || st.getDepartment().toLowerCase().contains(deptCode.toLowerCase()))) {
                            count++;
                        }
                    }
                }
                if (txtDeptStudentCount != null) txtDeptStudentCount.setText(String.valueOf(count));
            }

            @Override
            public void onError(Exception e) {
                if (txtDeptStudentCount != null) txtDeptStudentCount.setText("0");
            }
        });
    }
}
