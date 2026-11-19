package com.example.trackpoint;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.trackpoint.network.dto.DepartmentDto;
import com.example.trackpoint.network.dto.StaffAdminDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SuperAdminDashboard extends AppCompatActivity {

    private TextView txtCountDepartments, txtCountStaffAdmins, txtCountStudentAdmins, txtCountTotalStudents;
    private StudentRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_super_admin);

        repository = new StudentRepository(this);

        txtCountDepartments = findViewById(R.id.txtCountDepartments);
        txtCountStaffAdmins = findViewById(R.id.txtCountStaffAdmins);
        txtCountStudentAdmins = findViewById(R.id.txtCountStudentAdmins);
        txtCountTotalStudents = findViewById(R.id.txtCountTotalStudents);

        TextView txtSuperDashboardDate = findViewById(R.id.txtSuperDashboardDate);
        if (txtSuperDashboardDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("'Today, 'MMM dd, yyyy", Locale.US);
            txtSuperDashboardDate.setText(sdf.format(new Date()));
        }

        findViewById(R.id.btnSuperHeaderLogout).setOnClickListener(v -> showLogoutDialog());

        // Card Click Listeners
        View cardDepts = findViewById(R.id.cardSuperDepartments);
        if (cardDepts != null) {
            cardDepts.setOnClickListener(v -> startActivity(new Intent(SuperAdminDashboard.this, SuperAdminDepartments.class)));
        }

        View cardStaff = findViewById(R.id.cardSuperStaffAdmins);
        if (cardStaff != null) {
            cardStaff.setOnClickListener(v -> startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStaffAdmins.class)));
        }

        View cardStudentAdmins = findViewById(R.id.cardSuperStudentAdmins);
        if (cardStudentAdmins != null) {
            cardStudentAdmins.setOnClickListener(v -> startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStudentAdmins.class)));
        }

        View cardTotalStudents = findViewById(R.id.cardSuperTotalStudents);
        if (cardTotalStudents != null) {
            cardTotalStudents.setOnClickListener(v -> startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStudents.class)));
        }

        // Sidebar Menu
        findViewById(R.id.btnSuperMenu).setOnClickListener(v -> {
            View sidebarView = LayoutInflater.from(SuperAdminDashboard.this)
                    .inflate(R.layout.layout_super_admin_sidebar, null);

            PopupWindow sidebar = new PopupWindow(
                    sidebarView,
                    (int) (280 * getResources().getDisplayMetrics().density),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    true
            );

            sidebar.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            sidebar.setOutsideTouchable(true);
            sidebar.setElevation(20);
            sidebar.showAtLocation(findViewById(android.R.id.content), Gravity.START, 0, 0);

            sidebarView.findViewById(R.id.menuSuperDashboard).setOnClickListener(item -> sidebar.dismiss());

            sidebarView.findViewById(R.id.menuSuperDepartments).setOnClickListener(item -> {
                startActivity(new Intent(SuperAdminDashboard.this, SuperAdminDepartments.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuSuperStaffAdmins).setOnClickListener(item -> {
                startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStaffAdmins.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuSuperStudentAdmins).setOnClickListener(item -> {
                startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStudentAdmins.class));
                sidebar.dismiss();
            });

            View menuStudents = sidebarView.findViewById(R.id.menuSuperStudents);
            if (menuStudents != null) {
                menuStudents.setOnClickListener(item -> {
                    startActivity(new Intent(SuperAdminDashboard.this, SuperAdminStudents.class));
                    sidebar.dismiss();
                });
            }

            View headerProfile = sidebarView.findViewById(R.id.headerSuperProfile);
            if (headerProfile != null) {
                headerProfile.setOnClickListener(item -> {
                    startActivity(new Intent(SuperAdminDashboard.this, SuperAdminProfileActivity.class));
                    sidebar.dismiss();
                });
            }

            View menuProfile = sidebarView.findViewById(R.id.menuSuperProfile);
            if (menuProfile != null) {
                menuProfile.setOnClickListener(item -> {
                    startActivity(new Intent(SuperAdminDashboard.this, SuperAdminProfileActivity.class));
                    sidebar.dismiss();
                });
            }

            sidebarView.findViewById(R.id.menuSuperLogout).setOnClickListener(item -> {
                sidebar.dismiss();
                showLogoutDialog();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSuperAdminDashboardMetrics();
    }

    private void loadSuperAdminDashboardMetrics() {
        TrackPointRepository cloudRepo = TrackPointRepository.getInstance(this);

        cloudRepo.getAllDepartments(new TrackPointRepository.RepositoryCallback<List<DepartmentDto>>() {
            @Override
            public void onSuccess(List<DepartmentDto> result) {
                if (txtCountDepartments != null && result != null) {
                    txtCountDepartments.setText(String.valueOf(result.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                if (txtCountDepartments != null) txtCountDepartments.setText("0");
            }
        });

        cloudRepo.getAllStaffAdmins(new TrackPointRepository.RepositoryCallback<List<StaffAdminDto>>() {
            @Override
            public void onSuccess(List<StaffAdminDto> result) {
                if (txtCountStaffAdmins != null && result != null) {
                    txtCountStaffAdmins.setText(String.valueOf(result.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                if (txtCountStaffAdmins != null) txtCountStaffAdmins.setText("0");
            }
        });

        cloudRepo.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> result) {
                if (txtCountStudentAdmins != null && result != null) {
                    txtCountStudentAdmins.setText(String.valueOf(result.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                if (txtCountStudentAdmins != null) txtCountStudentAdmins.setText("0");
            }
        });

        cloudRepo.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> result) {
                if (txtCountTotalStudents != null && result != null) {
                    txtCountTotalStudents.setText(String.valueOf(result.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                if (txtCountTotalStudents != null) txtCountTotalStudents.setText("0");
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(SuperAdminDashboard.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
