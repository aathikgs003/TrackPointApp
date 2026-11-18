package com.example.trackpoint;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StudentAttendanceCalendarActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView btnPrevMonth, btnNextMonth, tvCurrentMonth;
    GridLayout calendarGrid;

    int currentMonth;
    int currentYear;

    int todayDay;
    int todayMonth;
    int todayYear;

    TrackPointRepository repository;
    String studentId = "CSE22CS001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance_calendar);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        studentId = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        btnBack = findViewById(R.id.btnBack);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        calendarGrid = findViewById(R.id.calendarGrid);

        btnBack.setOnClickListener(v -> finish());

        // Default to current month and year
        Calendar todayCal = Calendar.getInstance();
        todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
        todayMonth = todayCal.get(Calendar.MONTH);
        todayYear = todayCal.get(Calendar.YEAR);

        currentMonth = todayMonth;
        currentYear = todayYear;

        updateCalendar();

        btnPrevMonth.setOnClickListener(v -> {
            if (currentMonth == 0) {
                currentMonth = 11;
                currentYear--;
            } else {
                currentMonth--;
            }
            updateCalendar();
        });

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
                statusMessage[0] = "Weekend Holiday";
                dayView.setTextColor(Color.parseColor("#7A8294"));
                dayView.setBackgroundResource(isToday ? R.drawable.bg_day_today : R.drawable.bg_day_holiday);
            } else {
                // Fetch live status from Neon PostgreSQL for logged-in student
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
            calendarGrid.addView(dayView);
        }
    }
}
