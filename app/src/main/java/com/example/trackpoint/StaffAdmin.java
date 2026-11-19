package com.example.trackpoint;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffAdmin extends AppCompatActivity {

    private TextView quickAdmins;
    private TextView tvStaffAdminsCount, tvStaffStudentsCount, tvStaffAttendanceRate, tvStaffSubmissionsCount;
    private TrackPointRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set Staff Admin status bar color
        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_staff_admin);

        repository = TrackPointRepository.getInstance(this);

        tvStaffAdminsCount = findViewById(R.id.tvStaffAdminsCount);
        tvStaffStudentsCount = findViewById(R.id.tvStaffStudentsCount);
        tvStaffAttendanceRate = findViewById(R.id.tvStaffAttendanceRate);
        tvStaffSubmissionsCount = findViewById(R.id.tvStaffSubmissionsCount);

        // Quick Access
        quickAdmins = findViewById(R.id.quickAdmins);

        TextView txtDashboardDate = findViewById(R.id.txtDashboardDate);
        if (txtDashboardDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("'Today, 'MMM dd, yyyy", Locale.US);
            txtDashboardDate.setText(sdf.format(new Date()));
        }

        // Top Header - Logout
        findViewById(R.id.btnStaffHeaderLogout).setOnClickListener(v -> showLogoutDialog());

        // Top Header - Side Menu
        findViewById(R.id.btnStaffMenu).setOnClickListener(v -> {
            View sidebarView = LayoutInflater.from(StaffAdmin.this)
                    .inflate(R.layout.layout_staff_sidebar, null);

            PopupWindow sidebar = new PopupWindow(
                    sidebarView,
                    (int) (280 * getResources().getDisplayMetrics().density),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    true
            );

            sidebar.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            sidebar.setOutsideTouchable(true);
            sidebar.setElevation(20);

            sidebar.showAtLocation(
                    findViewById(android.R.id.content),
                    Gravity.START,
                    0,
                    0
            );

            sidebarView.findViewById(R.id.menuStaffDashboard).setOnClickListener(item -> sidebar.dismiss());

            sidebarView.findViewById(R.id.menuStaffAdmins).setOnClickListener(item -> {
                startActivity(new Intent(StaffAdmin.this, StudentAdmins.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuStaffStudents).setOnClickListener(item -> {
                startActivity(new Intent(StaffAdmin.this, StaffAdminStudents.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuStaffReports).setOnClickListener(item -> {
                startActivity(new Intent(StaffAdmin.this, StaffReports.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuStaffProfile).setOnClickListener(item -> {
                startActivity(new Intent(StaffAdmin.this, StaffProfileActivity.class));
                sidebar.dismiss();
            });

            sidebarView.findViewById(R.id.menuStaffLogout).setOnClickListener(item -> {
                sidebar.dismiss();
                showLogoutDialog();
            });
        });

        // Quick Access - Student Admins
        if (quickAdmins != null) {
            quickAdmins.setOnClickListener(v -> startActivity(new Intent(StaffAdmin.this, StudentAdmins.class)));
        }

        // Quick Access - Students
        TextView quickStudents = findViewById(R.id.quickStudents);
        if (quickStudents != null) {
            quickStudents.setOnClickListener(v -> startActivity(new Intent(StaffAdmin.this, StaffAdminStudents.class)));
        }

        // Quick Access - Progress History
        TextView quickProgressHistory = findViewById(R.id.quickProgressHistory);
        if (quickProgressHistory != null) {
            quickProgressHistory.setOnClickListener(v -> startActivity(new Intent(StaffAdmin.this, StaffProgressHistory.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String loggedInStaffName = sessionPrefs.getString("logged_in_staff_name", "Suruthika J");
        String loggedInStaffId = sessionPrefs.getString("logged_in_staff_id", "STF001");

        repository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> adminsList) {
                int saCount = (adminsList != null && !adminsList.isEmpty()) ? adminsList.size() : 1;
                if (tvStaffAdminsCount != null) {
                    tvStaffAdminsCount.setText(String.valueOf(saCount));
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> studentList) {
                int totalStudents = studentList != null ? studentList.size() : 0;

                if (tvStaffStudentsCount != null) {
                    tvStaffStudentsCount.setText(String.valueOf(totalStudents));
                }

                if (studentList == null || studentList.isEmpty()) {
                    if (tvStaffSubmissionsCount != null) tvStaffSubmissionsCount.setText("0");
                    if (tvStaffAttendanceRate != null) tvStaffAttendanceRate.setText("100%");
                    return;
                }

                SimpleDateFormat dateKeyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
                String todayKey = dateKeyFmt.format(new Date());

                final int totalCount = studentList.size();
                final int[] submissionsSubmitted = {0};
                final int[] completedQueries = {0};

                for (StudentDto student : studentList) {
                    String regNo = student.getRegisterNumber();
                    if (regNo == null || regNo.isEmpty()) {
                        completedQueries[0]++;
                        continue;
                    }

                    repository.getDailyActivity(regNo, todayKey, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
                        @Override
                        public void onSuccess(DailyActivityDto activity) {
                            if (activity != null) {
                                submissionsSubmitted[0]++;
                            }
                            checkDone();
                        }

                        @Override
                        public void onError(Exception e) {
                            checkDone();
                        }

                        private void checkDone() {
                            completedQueries[0]++;
                            if (completedQueries[0] >= totalCount) {
                                if (tvStaffSubmissionsCount != null) {
                                    tvStaffSubmissionsCount.setText(String.valueOf(submissionsSubmitted[0]));
                                }
                                int rate = totalCount > 0 ? (int) Math.round(((double) submissionsSubmitted[0] / totalCount) * 100) : 100;
                                if (tvStaffAttendanceRate != null) {
                                    tvStaffAttendanceRate.setText((rate > 0 ? rate : 100) + "%");
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(StaffAdmin.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
