package com.example.trackpoint;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProgressHistory extends AppCompatActivity {

    ImageView btnBack;
    Spinner spStudent;
    TextView tvFilterDate, tvProgressResultsTitle;
    LinearLayout layoutProgressContent;

    Calendar filterCalendar;
    TrackPointRepository repository;
    String loggedInAdminName = "Aathi";

    List<StudentDto> loadedAssignedStudents = new ArrayList<>();
    private int currentRenderRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_history);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        btnBack = findViewById(R.id.btnBack);
        spStudent = findViewById(R.id.spStudent);
        tvFilterDate = findViewById(R.id.tvFilterDate);
        tvProgressResultsTitle = findViewById(R.id.tvProgressResultsTitle);
        layoutProgressContent = findViewById(R.id.layoutProgressContent);

        btnBack.setOnClickListener(v -> finish());

        filterCalendar = Calendar.getInstance();
        updateDateDisplay();

        tvFilterDate.setOnClickListener(v -> showDatePicker());

        spStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderProgressResults();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadAssignedStudentsFromNeon();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignedStudentsFromNeon();
    }

    private void loadAssignedStudentsFromNeon() {
        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> studentList) {
                loadedAssignedStudents.clear();
                Set<String> seenRegNos = new HashSet<>();
                ArrayList<String> studentNames = new ArrayList<>();
                studentNames.add("All Assigned Students");

                if (studentList != null) {
                    for (StudentDto s : studentList) {
                        String regNo = s.getRegisterNumber();
                        if (regNo != null && !seenRegNos.contains(regNo.toLowerCase())) {
                            boolean isAssigned = s.getAssignedAdmin() == null ||
                                    s.getAssignedAdmin().equalsIgnoreCase(loggedInAdminName) ||
                                    loggedInAdminName.equalsIgnoreCase("Aathi");

                            if (isAssigned) {
                                seenRegNos.add(regNo.toLowerCase());
                                loadedAssignedStudents.add(s);
                                studentNames.add(s.getName());
                            }
                        }
                    }
                }

                setSpinner(spStudent, studentNames.toArray(new String[0]));
                renderProgressResults();
            }

            @Override
            public void onError(Exception e) {
                renderProgressResults();
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        if (tvFilterDate != null) {
            tvFilterDate.setText("📅   " + sdf.format(filterCalendar.getTime()));
        }
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    filterCalendar.set(Calendar.YEAR, year);
                    filterCalendar.set(Calendar.MONTH, month);
                    filterCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                    renderProgressResults();
                },
                filterCalendar.get(Calendar.YEAR),
                filterCalendar.get(Calendar.MONTH),
                filterCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void renderProgressResults() {
        if (layoutProgressContent == null) return;
        layoutProgressContent.removeAllViews();

        final int requestId = ++currentRenderRequestId;
        final Set<String> renderedStudentRegs = new HashSet<>();

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        SimpleDateFormat displayFmt = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());

        String dateKey = keyFmt.format(filterCalendar.getTime());
        String dateDisplay = displayFmt.format(filterCalendar.getTime());

        if (tvProgressResultsTitle != null) {
            tvProgressResultsTitle.setText("Uploads for " + dateDisplay);
        }

        String selectedStudent = spStudent.getSelectedItem() != null ? spStudent.getSelectedItem().toString() : "All Assigned Students";

        if (loadedAssignedStudents.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No students assigned to " + loggedInAdminName + " in Neon database");
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(android.graphics.Color.parseColor("#68738A"));
            layoutProgressContent.addView(emptyView);
            return;
        }

        for (StudentDto s : loadedAssignedStudents) {
            if (!selectedStudent.equals("All Assigned Students") && !s.getName().equalsIgnoreCase(selectedStudent)) {
                continue;
            }

            final String regNo = s.getRegisterNumber();

            // Query Neon PostgreSQL database table daily_activities for this student and date
            repository.getDailyActivity(regNo, dateKey, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
                @Override
                public void onSuccess(DailyActivityDto activity) {
                    if (requestId != currentRenderRequestId) return;
                    if (renderedStudentRegs.contains(regNo.toLowerCase())) return;

                    renderedStudentRegs.add(regNo.toLowerCase());

                    boolean isSubmitted = activity != null && ((activity.getFileName() != null && !activity.getFileName().isEmpty()) || (activity.getNotes() != null && !activity.getNotes().isEmpty()));

                    String fileName = activity != null && activity.getFileName() != null && !activity.getFileName().isEmpty() ? activity.getFileName() : "No File Uploaded";
                    String notes = activity != null && activity.getNotes() != null ? activity.getNotes() : "No activity document or description submitted for this date.";
                    String timeStr = activity != null && activity.getSubmitTime() != null ? activity.getSubmitTime() : "Not Submitted";

                    layoutProgressContent.addView(createStudentUploadCard(s, dateDisplay, fileName, notes, timeStr, isSubmitted));
                }

                @Override
                public void onError(Exception e) {
                    if (requestId != currentRenderRequestId) return;
                    if (renderedStudentRegs.contains(regNo.toLowerCase())) return;

                    renderedStudentRegs.add(regNo.toLowerCase());
                    layoutProgressContent.addView(createStudentUploadCard(s, dateDisplay, "No File Uploaded", "No activity document or description submitted for this date.", "Not Submitted", false));
                }
            });
        }
    }

    private View createStudentUploadCard(StudentDto student, String dateStr, String fileName, String notesStr, String timeStr, boolean isSubmitted) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_report_card, layoutProgressContent, false);

        TextView tvDate = card.findViewById(R.id.tvReportCardDate);
        TextView tvTime = card.findViewById(R.id.tvReportCardTime);

        LinearLayout layoutBadge = card.findViewById(R.id.layoutReportBadge);
        ImageView imgBadgeIcon = card.findViewById(R.id.imgReportBadgeIcon);
        TextView tvBadgeText = card.findViewById(R.id.tvReportBadgeText);

        TextView tvDocName = card.findViewById(R.id.tvReportDocName);
        TextView tvNotes = card.findViewById(R.id.tvReportNotes);

        if (tvDate != null) tvDate.setText("👤 " + student.getName() + "   (" + dateStr + ")");
        if (tvTime != null) tvTime.setText(timeStr);

        if (isSubmitted) {
            if (layoutBadge != null) layoutBadge.setBackgroundResource(R.drawable.card_green);
            if (imgBadgeIcon != null) {
                imgBadgeIcon.setImageResource(R.drawable.check_circle_symbol);
                imgBadgeIcon.setColorFilter(Color.parseColor("#10B23E"));
            }
            if (tvBadgeText != null) {
                tvBadgeText.setText("Verified Processing");
                tvBadgeText.setTextColor(Color.parseColor("#10B23E"));
            }
            if (tvDocName != null) {
                tvDocName.setText("📄 " + fileName);
                tvDocName.setTextColor(Color.parseColor("#18233F"));
            }
        } else {
            if (layoutBadge != null) layoutBadge.setBackgroundResource(R.drawable.card_yellow);
            if (imgBadgeIcon != null) {
                imgBadgeIcon.setImageResource(R.drawable.pending_symbol);
                imgBadgeIcon.setColorFilter(Color.parseColor("#D97706"));
            }
            if (tvBadgeText != null) {
                tvBadgeText.setText("Not Submitted");
                tvBadgeText.setTextColor(Color.parseColor("#D97706"));
            }
            if (tvDocName != null) {
                tvDocName.setText("⚠️ " + fileName);
                tvDocName.setTextColor(Color.parseColor("#68738A"));
            }
        }

        if (tvNotes != null) {
            tvNotes.setText(notesStr.isEmpty() ? "No description provided" : notesStr);
        }

        String fileType = (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) ? "Image" : "PDF";

        card.setOnClickListener(v -> {
            Intent intent = new Intent(ProgressHistory.this, StaffSubmissionDetails.class);
            intent.putExtra("studentName", student.getName());
            intent.putExtra("studentId", student.getRegisterNumber());
            intent.putExtra("department", student.getDepartment());
            intent.putExtra("year", student.getYear());
            intent.putExtra("assignedAdmin", student.getAssignedAdmin() != null ? student.getAssignedAdmin() : loggedInAdminName);
            intent.putExtra("submittedDate", dateStr);
            intent.putExtra("notes", notesStr);
            intent.putExtra("fileName", fileName);
            intent.putExtra("fileType", fileType);
            intent.putExtra("isSubmitted", isSubmitted);
            startActivity(intent);
        });

        return card;
    }

    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                items
        );

        adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }
}
