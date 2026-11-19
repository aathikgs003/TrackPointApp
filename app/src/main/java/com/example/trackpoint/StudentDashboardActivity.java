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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    TextView tvStudentGreeting, tvStudentRegNo, tvHeaderSubDeptYear;
    TextView tvStudentAttendance, tvCompletedTasks, tvInProgressTasks, tvTotalTasks;
    TextView tvTodayDate, tvDashboardStatusTitle, tvDashboardSubmitTime, tvDashboardFileName;
    ImageView imgDashboardStatusIcon;
    LinearLayout cardTodayActivityStatus, layoutDashboardStatusBadge;

    TrackPointRepository repository;
    String loggedInRegNo = "CSE22CS001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        repository = TrackPointRepository.getInstance(this);

        tvStudentGreeting = findViewById(R.id.tvStudentGreeting);
        tvStudentRegNo = findViewById(R.id.tvStudentRegNo);
        tvHeaderSubDeptYear = findViewById(R.id.tvHeaderSubDeptYear);

        tvStudentAttendance = findViewById(R.id.tvStudentAttendance);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvInProgressTasks = findViewById(R.id.tvInProgressTasks);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);

        // Header Logout Button
        findViewById(R.id.notificationButton).setOnClickListener(v -> showLogoutDialog());

        // Summary Cards
        View cardAttendance = findViewById(R.id.cardAttendance);
        View cardCompleted = findViewById(R.id.cardCompleted);
        View cardInProgress = findViewById(R.id.cardInProgress);
        View cardTotalTasks = findViewById(R.id.cardTotalTasks);

        if (cardAttendance != null) cardAttendance.setOnClickListener(null);
        if (cardCompleted != null) cardCompleted.setOnClickListener(null);
        if (cardInProgress != null) cardInProgress.setOnClickListener(null);
        if (cardTotalTasks != null) cardTotalTasks.setOnClickListener(null);

        // Today's Activity Views
        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvDashboardStatusTitle = findViewById(R.id.tvDashboardStatusTitle);
        tvDashboardSubmitTime = findViewById(R.id.tvDashboardSubmitTime);
        tvDashboardFileName = findViewById(R.id.tvDashboardFileName);

        imgDashboardStatusIcon = findViewById(R.id.imgDashboardStatusIcon);
        cardTodayActivityStatus = findViewById(R.id.cardTodayActivityStatus);
        layoutDashboardStatusBadge = findViewById(R.id.layoutDashboardStatusBadge);

        // Sidebar Navigation
        findViewById(R.id.moreButton).setOnClickListener(v -> showSidebar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentInfo();
        loadTodayActivityStatus();
    }

    private void loadStudentInfo() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");
        String sessionName = sessionPrefs.getString("logged_in_user_name", "");

        if (tvStudentGreeting != null) {
            tvStudentGreeting.setText("Hello, " + (sessionName.isEmpty() ? "Student" : sessionName) + " 👋");
        }
        if (tvStudentRegNo != null) {
            tvStudentRegNo.setText("Reg No: " + loggedInRegNo);
        }

        repository.getStudentByRegNo(loggedInRegNo, new TrackPointRepository.RepositoryCallback<StudentDto>() {
            @Override
            public void onSuccess(StudentDto student) {
                if (student != null) {
                    if (tvStudentGreeting != null) {
                        tvStudentGreeting.setText("Hello, " + student.getName() + " 👋");
                    }
                    if (tvStudentRegNo != null) {
                        tvStudentRegNo.setText("Reg No: " + student.getRegisterNumber());
                    }
                    if (tvHeaderSubDeptYear != null) {
                        tvHeaderSubDeptYear.setText(student.getDepartment() + " Department - " + student.getYear());
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (tvStudentGreeting != null) tvStudentGreeting.setText("Hello Student 👋");
                if (tvStudentRegNo != null) tvStudentRegNo.setText("Reg No: " + loggedInRegNo);
            }
        });
    }

    private void loadTodayActivityStatus() {
        SimpleDateFormat displayDateFmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat dateKeyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());

        Date today = new Date();
        String dateDisplayStr = displayDateFmt.format(today);
        String dateKeyStr = dateKeyFmt.format(today);

        if (tvTodayDate != null) {
            tvTodayDate.setText(dateDisplayStr);
        }

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        SharedPreferences prefs = getSharedPreferences("TodayActivityPrefs", MODE_PRIVATE);

        repository.getDailyActivity(loggedInRegNo, dateKeyStr, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
            @Override
            public void onSuccess(DailyActivityDto activity) {
                boolean isSubmitted = (activity != null && ((activity.getFileName() != null && !activity.getFileName().isEmpty()) || (activity.getNotes() != null && !activity.getNotes().isEmpty())));

                String fileName = activity != null && activity.getFileName() != null && !activity.getFileName().isEmpty() ? activity.getFileName() : "Document Uploaded";
                String submitTime = activity != null && activity.getSubmitTime() != null && !activity.getSubmitTime().isEmpty() ? activity.getSubmitTime() : "Submitted Today";

                updateDashboardSubmissionUI(isSubmitted, fileName, submitTime);
            }

            @Override
            public void onError(Exception e) {
                boolean localSubmitted = prefs.getBoolean("submitted_" + loggedInRegNo + "_" + dateKeyStr, false);
                String localFileName = prefs.getString("file_name_" + loggedInRegNo + "_" + dateKeyStr, "Document Uploaded");
                String localSubmitTime = prefs.getString("submit_time_" + loggedInRegNo + "_" + dateKeyStr, "Submitted Today");
                updateDashboardSubmissionUI(localSubmitted, localFileName, localSubmitTime);
            }
        });

        if (cardTodayActivityStatus != null) {
            cardTodayActivityStatus.setOnClickListener(v ->
                    startActivity(new Intent(this, StudentTodayActivity.class))
            );
        }
    }

    private void updateDashboardSubmissionUI(boolean isSubmitted, String fileName, String submitTime) {
        int completed = isSubmitted ? 1 : 0;
        int inProgress = isSubmitted ? 0 : 1;
        int totalTasks = 1;

        if (tvCompletedTasks != null) tvCompletedTasks.setText(String.valueOf(completed));
        if (tvInProgressTasks != null) tvInProgressTasks.setText(String.valueOf(inProgress));
        if (tvTotalTasks != null) tvTotalTasks.setText(String.valueOf(totalTasks));
        if (tvStudentAttendance != null) tvStudentAttendance.setText(isSubmitted ? "100%" : "95%");

        if (isSubmitted) {
            if (tvDashboardStatusTitle != null) {
                tvDashboardStatusTitle.setText("Submitted Today");
                tvDashboardStatusTitle.setTextColor(Color.parseColor("#10B23E"));
            }
            if (imgDashboardStatusIcon != null) {
                imgDashboardStatusIcon.setImageResource(R.drawable.check_circle_symbol);
                imgDashboardStatusIcon.setColorFilter(Color.parseColor("#10B23E"));
            }
            if (layoutDashboardStatusBadge != null) {
                layoutDashboardStatusBadge.setBackgroundResource(R.drawable.card_green);
            }
            if (tvDashboardFileName != null) {
                tvDashboardFileName.setText("📄 " + (fileName == null || fileName.isEmpty() ? "Document Uploaded" : fileName));
                tvDashboardFileName.setTextColor(Color.parseColor("#18233F"));
            }
            if (tvDashboardSubmitTime != null) {
                tvDashboardSubmitTime.setText(submitTime == null || submitTime.isEmpty() ? "Submitted Today" : submitTime);
            }
        } else {
            if (tvDashboardStatusTitle != null) {
                tvDashboardStatusTitle.setText("Not Submitted Today");
                tvDashboardStatusTitle.setTextColor(Color.parseColor("#D97706"));
            }
            if (imgDashboardStatusIcon != null) {
                imgDashboardStatusIcon.setImageResource(R.drawable.pending_symbol);
                imgDashboardStatusIcon.setColorFilter(Color.parseColor("#D97706"));
            }
            if (layoutDashboardStatusBadge != null) {
                layoutDashboardStatusBadge.setBackgroundResource(R.drawable.card_yellow);
            }
            if (tvDashboardFileName != null) {
                tvDashboardFileName.setText("⚠️ You haven't submitted today's activity yet.");
                tvDashboardFileName.setTextColor(Color.parseColor("#68738A"));
            }
            if (tvDashboardSubmitTime != null) {
                tvDashboardSubmitTime.setText("Tap to submit");
            }
        }
    }

    private void showSidebar() {
        View sidebarView = LayoutInflater.from(this)
                .inflate(R.layout.layout_user_student_sidebar, null);

        TextView tvStudentSidebarName = sidebarView.findViewById(R.id.tvStudentSidebarName);
        TextView tvStudentSidebarSub = sidebarView.findViewById(R.id.tvStudentSidebarSub);

        repository.getStudentByRegNo(loggedInRegNo, new TrackPointRepository.RepositoryCallback<StudentDto>() {
            @Override
            public void onSuccess(StudentDto student) {
                if (student != null) {
                    if (tvStudentSidebarName != null) tvStudentSidebarName.setText(student.getName());
                    if (tvStudentSidebarSub != null) tvStudentSidebarSub.setText(student.getDepartment() + " Department - " + student.getYear());
                }
            }

            @Override
            public void onError(Exception e) {
                if (tvStudentSidebarName != null) tvStudentSidebarName.setText("Student");
            }
        });

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

        sidebarView.findViewById(R.id.menuHome).setOnClickListener(v -> sidebar.dismiss());
        sidebarView.findViewById(R.id.menuTodayActivity).setOnClickListener(v -> {
            sidebar.dismiss();
            startActivity(new Intent(this, StudentTodayActivity.class));
        });
        sidebarView.findViewById(R.id.menuAttendance).setOnClickListener(v -> {
            sidebar.dismiss();
            startActivity(new Intent(this, StudentAttendanceActivity.class));
        });
        sidebarView.findViewById(R.id.menuReports).setOnClickListener(v -> {
            sidebar.dismiss();
            startActivity(new Intent(this, StudentReportsActivity.class));
        });
        sidebarView.findViewById(R.id.menuProfile).setOnClickListener(v -> {
            sidebar.dismiss();
            startActivity(new Intent(this, StudentProfileActivity.class));
        });
        sidebarView.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            sidebar.dismiss();
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(StudentDashboardActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
