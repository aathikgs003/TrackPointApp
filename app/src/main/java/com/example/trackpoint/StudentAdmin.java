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

import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentAdmin extends AppCompatActivity {

    TextView tvStudentCount, tvAttendance, tvProgressCount, tvPending;
    TextView tvAdminGreeting, tvAdminSubDept;
    TrackPointRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_admin_dashboard);

        repository = TrackPointRepository.getInstance(this);

        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvAttendance = findViewById(R.id.tvAttendance);
        tvProgressCount = findViewById(R.id.tvProgressCount);
        tvPending = findViewById(R.id.tvPending);
        tvAdminGreeting = findViewById(R.id.tvAdminGreeting);
        tvAdminSubDept = findViewById(R.id.tvAdminSubDept);

        // Quick Action Buttons
        findViewById(R.id.btnProgressHistory).setOnClickListener(v -> {
            startActivity(new Intent(StudentAdmin.this, ProgressHistory.class));
        });

        findViewById(R.id.btnReports).setOnClickListener(v -> {
            startActivity(new Intent(StudentAdmin.this, Reports.class));
        });

        // Header Logout Button
        if (findViewById(R.id.notificationButton) != null) {
            findViewById(R.id.notificationButton).setOnClickListener(v -> showLogoutDialog());
        }

        // Sidebar Navigation
        View menuButton = findViewById(R.id.moreButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> showSidebar());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDynamicDashboardData();
    }

    private void loadDynamicDashboardData() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");
        String loggedInAdminId = sessionPrefs.getString("logged_in_admin_id", "SADM001");

        final String adminName = loggedInAdminName;

        if (tvAdminGreeting != null) {
            tvAdminGreeting.setText(adminName);
        }

        // Fetch Student Admin details from Neon PostgreSQL
        repository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> admins) {
                if (admins != null) {
                    for (StudentAdminDto sa : admins) {
                        if (loggedInAdminId.equalsIgnoreCase(sa.getAdminId()) || adminName.equalsIgnoreCase(sa.getName())) {
                            if (tvAdminSubDept != null) {
                                tvAdminSubDept.setText(sa.getDepartment() + " Department");
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (tvAdminSubDept != null) tvAdminSubDept.setText("CSE Department");
            }
        });

        // Fetch Assigned Students Count and Daily Activities from Neon PostgreSQL
        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> studentList) {
                int assignedCount = 0;
                if (studentList != null) {
                    for (StudentDto s : studentList) {
                        if (s.getAssignedAdmin() == null || s.getAssignedAdmin().equalsIgnoreCase(adminName) || adminName.equalsIgnoreCase("Aathi")) {
                            assignedCount++;
                        }
                    }
                }

                if (tvStudentCount != null) {
                    tvStudentCount.setText(String.valueOf(assignedCount));
                }

                SimpleDateFormat dateKeyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
                String todayKeyStr = dateKeyFmt.format(new Date());

                int finalAssignedCount = assignedCount;

                fetchTodayActivitiesCount(studentList, adminName, todayKeyStr, finalAssignedCount);
            }

            @Override
            public void onError(Exception e) {
                if (tvStudentCount != null) tvStudentCount.setText("0");
            }
        });
    }

    private void fetchTodayActivitiesCount(List<StudentDto> studentList, String adminName, String todayKeyStr, int assignedCount) {
        if (studentList == null || studentList.isEmpty()) {
            if (tvPending != null) tvPending.setText("0");
            if (tvAttendance != null) tvAttendance.setText("100%");
            return;
        }

        final int[] submittedToday = {0};

        for (StudentDto s : studentList) {
            if (s.getAssignedAdmin() == null || s.getAssignedAdmin().equalsIgnoreCase(adminName) || adminName.equalsIgnoreCase("Aathi")) {
                repository.getDailyActivity(s.getRegisterNumber(), todayKeyStr, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
                    @Override
                    public void onSuccess(DailyActivityDto activity) {
                        if (activity != null && ((activity.getFileName() != null && !activity.getFileName().isEmpty()) || (activity.getNotes() != null && !activity.getNotes().isEmpty()))) {
                            submittedToday[0]++;
                        }
                        updateMetrics(submittedToday[0], assignedCount);
                    }

                    @Override
                    public void onError(Exception e) {
                        updateMetrics(submittedToday[0], assignedCount);
                    }
                });
            }
        }
    }

    private void updateMetrics(int submittedToday, int totalAssigned) {
        int pending = totalAssigned > 0 ? (totalAssigned - submittedToday) : 0;
        if (pending < 0) pending = 0;

        if (tvProgressCount != null) tvProgressCount.setText(String.valueOf(submittedToday));
        if (tvPending != null) tvPending.setText(String.valueOf(pending));
        if (tvAttendance != null) tvAttendance.setText("95%");
    }

    private void showSidebar() {
        View sidebarView = LayoutInflater.from(StudentAdmin.this)
                .inflate(R.layout.layout_student_sidebar, null);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        TextView tvSidebarAdminName = sidebarView.findViewById(R.id.tvSidebarAdminName);
        if (tvSidebarAdminName != null) tvSidebarAdminName.setText(loggedInAdminName);

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
                getWindow().getDecorView(),
                Gravity.START | Gravity.TOP,
                0,
                0
        );

        sidebarView.findViewById(R.id.menuDashboard).setOnClickListener(item -> sidebar.dismiss());
        sidebarView.findViewById(R.id.menuStudents).setOnClickListener(item -> {
            startActivity(new Intent(StudentAdmin.this, MyStudents.class));
            sidebar.dismiss();
        });

        sidebarView.findViewById(R.id.menuAttendance).setOnClickListener(item -> {
            startActivity(new Intent(StudentAdmin.this, MarkAttendance.class));
            sidebar.dismiss();
        });

        if (sidebarView.findViewById(R.id.menuProgressHistory) != null) {
            sidebarView.findViewById(R.id.menuProgressHistory).setOnClickListener(item -> {
                startActivity(new Intent(StudentAdmin.this, ProgressHistory.class));
                sidebar.dismiss();
            });
        }

        sidebarView.findViewById(R.id.menuReports).setOnClickListener(item -> {
            startActivity(new Intent(StudentAdmin.this, Reports.class));
            sidebar.dismiss();
        });

        sidebarView.findViewById(R.id.menuProfile).setOnClickListener(item -> {
            startActivity(new Intent(StudentAdmin.this, MyProfile.class));
            sidebar.dismiss();
        });

        sidebarView.findViewById(R.id.menuLogout).setOnClickListener(item -> {
            sidebar.dismiss();
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(StudentAdmin.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
