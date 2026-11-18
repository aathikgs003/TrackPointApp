package com.example.trackpoint;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StudentAttendanceActivity extends AppCompatActivity {

    ImageView btnBack, imgTodayStatusIcon;
    TextView tvTodayDate, tvTodayStatus, tvTodayNote;
    LinearLayout layoutTodayBadge;

    TextView btnPrevMonth, btnNextMonth, tvCurrentMonth;
    GridLayout calendarGrid;

    int currentMonth;
    int currentYear;

    int todayDay;
    int todayMonth;
    int todayYear;

    TrackPointRepository repository;
    String studentId = "CSE22CS001";
    String assignedAdminName = "Aathi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        studentId = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        btnBack = findViewById(R.id.btnBack);

        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        tvTodayNote = findViewById(R.id.tvTodayNote);
        imgTodayStatusIcon = findViewById(R.id.imgTodayStatusIcon);
        layoutTodayBadge = findViewById(R.id.layoutTodayBadge);

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        calendarGrid = findViewById(R.id.calendarGrid);

        btnBack.setOnClickListener(v -> finish());

        // Month Navigation
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                if (currentMonth == 0) {
                    currentMonth = 11;
                    currentYear--;
                } else {
                    currentMonth--;
                }
                updateCalendar();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                if (currentMonth == 11) {
                    currentMonth = 0;
                    currentYear++;
                } else {
                    currentMonth++;
                }
                updateCalendar();
            });
        }

        // Initialize today's date info
        Calendar todayCal = Calendar.getInstance();
        todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
        todayMonth = todayCal.get(Calendar.MONTH);
        todayYear = todayCal.get(Calendar.YEAR);

        currentMonth = todayMonth;
        currentYear = todayYear;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentDetails();
        loadTodayAttendance();
        updateCalendar();
    }

    private void loadStudentDetails() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        studentId = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        repository.getStudentByRegNo(studentId, new TrackPointRepository.RepositoryCallback<StudentDto>() {
            @Override
            public void onSuccess(StudentDto student) {
                if (student != null && student.getAssignedAdmin() != null && !student.getAssignedAdmin().isEmpty()) {
                    assignedAdminName = student.getAssignedAdmin();
                } else {
                    assignedAdminName = "Aathi";
                }
            }

            @Override
            public void onError(Exception e) {
                assignedAdminName = "Aathi";
            }
        });
    }

    private void loadTodayAttendance() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
        Date today = new Date();

        String currentDateStr = sdf.format(today);
        String todayKeyStr = keyFmt.format(today);

        if (tvTodayDate != null) {
            tvTodayDate.setText(currentDateStr);
        }

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        repository.getAttendanceStatus(studentId, todayKeyStr, new TrackPointRepository.RepositoryCallback<AttendanceDto>() {
            @Override
            public void onSuccess(AttendanceDto attendance) {
                String status = attendance != null ? attendance.getStatus() : null;
                updateTodayStatusUI(status, dayOfWeek);
            }

            @Override
            public void onError(Exception e) {
                updateTodayStatusUI(null, dayOfWeek);
            }
        });
    }

    private void updateTodayStatusUI(String status, int dayOfWeek) {
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);

        if (isWeekend) {
            if (tvTodayStatus != null) {
                tvTodayStatus.setText("WEEKEND HOLIDAY");
                tvTodayStatus.setTextColor(Color.parseColor("#7A8294"));
            }
            if (tvTodayNote != null) {
                tvTodayNote.setText("Weekend Holiday");
            }
            if (imgTodayStatusIcon != null) {
                imgTodayStatusIcon.setImageResource(R.drawable.timeline_dot);
                imgTodayStatusIcon.setColorFilter(Color.parseColor("#7A8294"));
            }
            if (layoutTodayBadge != null) {
                layoutTodayBadge.setBackgroundResource(R.drawable.bg_day_today);
            }
        } else if ("Present".equalsIgnoreCase(status)) {
            if (tvTodayStatus != null) {
                tvTodayStatus.setText("PRESENT");
                tvTodayStatus.setTextColor(Color.parseColor("#10B23E"));
            }
            if (tvTodayNote != null) {
                tvTodayNote.setText("Marked Present by " + assignedAdminName);
            }
            if (imgTodayStatusIcon != null) {
                imgTodayStatusIcon.setImageResource(R.drawable.check_circle_symbol);
                imgTodayStatusIcon.setColorFilter(Color.parseColor("#10B23E"));
            }
            if (layoutTodayBadge != null) {
                layoutTodayBadge.setBackgroundResource(R.drawable.bg_day_today_present);
            }
        } else if ("Absent".equalsIgnoreCase(status)) {
            if (tvTodayStatus != null) {
                tvTodayStatus.setText("ABSENT");
                tvTodayStatus.setTextColor(Color.parseColor("#D94343"));
            }
            if (tvTodayNote != null) {
                tvTodayNote.setText("Marked Absent by " + assignedAdminName);
            }
            if (imgTodayStatusIcon != null) {
                imgTodayStatusIcon.setImageResource(R.drawable.radio_button_symbol);
                imgTodayStatusIcon.setColorFilter(Color.parseColor("#D94343"));
            }
            if (layoutTodayBadge != null) {
                layoutTodayBadge.setBackgroundResource(R.drawable.bg_day_today_absent);
            }
        } else {
            // Not Mentioned / Unmarked Weekday
            if (tvTodayStatus != null) {
                tvTodayStatus.setText("NOT MENTIONED");
                tvTodayStatus.setTextColor(Color.parseColor("#4935B8"));
            }
            if (tvTodayNote != null) {
                tvTodayNote.setText("Not Mentioned");
            }
            if (imgTodayStatusIcon != null) {
                imgTodayStatusIcon.setImageResource(R.drawable.timeline_dot);
                imgTodayStatusIcon.setColorFilter(Color.parseColor("#4935B8"));
            }
            if (layoutTodayBadge != null) {
                layoutTodayBadge.setBackgroundResource(R.drawable.bg_day_today);
            }
        }
    }

    private void updateCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (tvCurrentMonth != null) {
            tvCurrentMonth.setText(sdf.format(cal.getTime()));
        }

        if (calendarGrid == null) return;
        calendarGrid.removeAllViews();

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float density = getResources().getDisplayMetrics().density;
        int cellHeight = (int) (44 * density);
        int margin = (int) (3 * density);

        for (int i = 1; i < firstDayOfWeek; i++) {
            View emptyView = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellHeight;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            emptyView.setLayoutParams(params);
            calendarGrid.addView(emptyView);
        }

        SimpleDateFormat dateKeyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.US);

        for (int day = 1; day <= totalDays; day++) {
            TextView dayView = new TextView(this);
            dayView.setText(String.valueOf(day));
            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(14f);
            dayView.setTypeface(null, android.graphics.Typeface.BOLD);

            cal.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
            boolean isToday = (currentYear == todayYear && currentMonth == todayMonth && day == todayDay);

            String dateKeyStr = dateKeyFmt.format(cal.getTime());
            SimpleDateFormat daySdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            String dateStr = daySdf.format(cal.getTime());

            // Default style: Grey Holiday / Unmarked Background or Blue Today Highlight
            if (isToday) {
                dayView.setTextColor(Color.parseColor("#4935B8"));
                dayView.setBackgroundResource(R.drawable.bg_day_today);
            } else {
                dayView.setTextColor(Color.parseColor("#7A8294"));
                dayView.setBackgroundResource(R.drawable.bg_day_holiday);
            }

            final String[] statusMessage = {isWeekend ? "Weekend Holiday" : "Not Mentioned"};

            dayView.setOnClickListener(v ->
                    Toast.makeText(this, dateStr + "\nStatus: " + statusMessage[0], Toast.LENGTH_SHORT).show()
            );

            if (isWeekend) {
                // Saturday and Sunday are Weekend Holidays
                statusMessage[0] = "Weekend Holiday";
                dayView.setTextColor(Color.parseColor("#7A8294"));
                dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today : R.drawable.bg_day_holiday);
            } else {
                // Fetch live status for weekday from Neon PostgreSQL
                repository.getAttendanceStatus(studentId, dateKeyStr, new TrackPointRepository.RepositoryCallback<AttendanceDto>() {
                    @Override
                    public void onSuccess(AttendanceDto attendance) {
                        if (attendance != null && attendance.getStatus() != null) {
                            String liveStatus = attendance.getStatus();
                            if ("Present".equalsIgnoreCase(liveStatus)) {
                                statusMessage[0] = "Present";
                                dayView.setTextColor(Color.parseColor("#10B23E"));
                                dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today_present : R.drawable.bg_day_present);
                            } else if ("Absent".equalsIgnoreCase(liveStatus)) {
                                statusMessage[0] = "Absent";
                                dayView.setTextColor(Color.parseColor("#D94343"));
                                dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today_absent : R.drawable.bg_day_absent);
                            } else {
                                statusMessage[0] = "Not Mentioned";
                                dayView.setTextColor(isToday ? Color.parseColor("#4935B8") : Color.parseColor("#7A8294"));
                                dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today : R.drawable.bg_day_holiday);
                            }
                        } else {
                            statusMessage[0] = "Not Mentioned";
                            dayView.setTextColor(isToday ? Color.parseColor("#4935B8") : Color.parseColor("#7A8294"));
                            dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today : R.drawable.bg_day_holiday);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        statusMessage[0] = "Not Mentioned";
                    }
                });
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellHeight;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            dayView.setLayoutParams(params);

            calendarGrid.addView(dayView);
        }
    }
}
