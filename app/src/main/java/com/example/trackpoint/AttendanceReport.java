package com.example.trackpoint;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AttendanceReport extends AppCompatActivity {

    ImageView btnBack;
    Spinner spStudent, spMonth;
    Button btnMonthlyMode, btnDateMode, btnDownloadPdf;
    LinearLayout layoutMonthContainer, layoutDateContainer, studentAttendanceList;
    TextView tvSelectDate, tvPercentage, tvTotalDays, tvPresentDays, tvAbsentDays, tvReportListTitle;

    boolean isMonthlyMode = true;
    Calendar selectedDateCal;

    TrackPointRepository repository;
    String loggedInAdminName = "Aathi";

    static class StudentReportItem {
        String studentName;
        String statusOrPercentage;
        boolean isPresent;

        StudentReportItem(String studentName, String statusOrPercentage, boolean isPresent) {
            this.studentName = studentName;
            this.statusOrPercentage = statusOrPercentage;
            this.isPresent = isPresent;
        }
    }

    ArrayList<String> monthOptionsList = new ArrayList<>();
    ArrayList<StudentReportItem> currentReportList = new ArrayList<>();
    List<StudentDto> loadedAssignedStudents = new ArrayList<>();
    private int reportRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        btnBack = findViewById(R.id.btnBack);
        spStudent = findViewById(R.id.spStudent);
        spMonth = findViewById(R.id.spMonth);

        btnMonthlyMode = findViewById(R.id.btnMonthlyMode);
        btnDateMode = findViewById(R.id.btnDateMode);

        layoutMonthContainer = findViewById(R.id.layoutMonthContainer);
        layoutDateContainer = findViewById(R.id.layoutDateContainer);
        tvSelectDate = findViewById(R.id.tvSelectDate);

        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);
        tvReportListTitle = findViewById(R.id.tvReportListTitle);

        studentAttendanceList = findViewById(R.id.studentAttendanceList);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);

        btnBack.setOnClickListener(v -> finish());

        // Generate Dynamic Month List
        buildDynamicMonthList();

        setSpinner(spMonth, monthOptionsList.toArray(new String[0]));

        // Select Current Month by Default
        SimpleDateFormat currentMonthFmt = new SimpleDateFormat("MMMM yyyy", Locale.US);
        String currentMonthStr = currentMonthFmt.format(new Date());
        for (int i = 0; i < monthOptionsList.size(); i++) {
            if (monthOptionsList.get(i).equalsIgnoreCase(currentMonthStr)) {
                spMonth.setSelection(i);
                break;
            }
        }

        selectedDateCal = Calendar.getInstance();
        updateDateText();

        btnMonthlyMode.setOnClickListener(v -> {
            isMonthlyMode = true;
            btnMonthlyMode.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4935B8")));
            btnMonthlyMode.setTextColor(Color.WHITE);

            btnDateMode.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAE5FF")));
            btnDateMode.setTextColor(Color.parseColor("#4935B8"));

            if (layoutMonthContainer != null) layoutMonthContainer.setVisibility(View.VISIBLE);
            if (layoutDateContainer != null) layoutDateContainer.setVisibility(View.GONE);

            updateReport();
        });

        btnDateMode.setOnClickListener(v -> {
            isMonthlyMode = false;
            btnDateMode.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4935B8")));
            btnDateMode.setTextColor(Color.WHITE);

            btnMonthlyMode.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAE5FF")));
            btnMonthlyMode.setTextColor(Color.parseColor("#4935B8"));

            if (layoutMonthContainer != null) layoutMonthContainer.setVisibility(View.GONE);
            if (layoutDateContainer != null) layoutDateContainer.setVisibility(View.VISIBLE);

            updateReport();
        });

        tvSelectDate.setOnClickListener(v -> showDatePicker());

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateReport();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spStudent.setOnItemSelectedListener(spinnerListener);
        spMonth.setOnItemSelectedListener(spinnerListener);

        if (btnDownloadPdf != null) {
            btnDownloadPdf.setOnClickListener(v -> generateAndDownloadPdf());
        }

        loadAssignedStudentsAndReport();
    }

    private void loadAssignedStudentsAndReport() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String role = sessionPrefs.getString("logged_in_role", "STUDENT_ADMIN");

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> studentList) {
                loadedAssignedStudents.clear();
                Set<String> seenRegs = new HashSet<>();
                ArrayList<String> studentNames = new ArrayList<>();
                studentNames.add("All Assigned Students");

                if (studentList != null) {
                    for (StudentDto s : studentList) {
                        String reg = s.getRegisterNumber();
                        if (reg != null && !seenRegs.contains(reg.toLowerCase())) {
                            boolean isAssigned = !"STUDENT_ADMIN".equalsIgnoreCase(role) ||
                                    (s.getAssignedAdmin() == null || s.getAssignedAdmin().equalsIgnoreCase(loggedInAdminName) || loggedInAdminName.equalsIgnoreCase("Aathi"));

                            if (isAssigned) {
                                seenRegs.add(reg.toLowerCase());
                                loadedAssignedStudents.add(s);
                                studentNames.add(s.getName());
                            }
                        }
                    }
                }
                setSpinner(spStudent, studentNames.toArray(new String[0]));
                updateReport();
            }

            @Override
            public void onError(Exception e) {
                updateReport();
            }
        });
    }

    private void buildDynamicMonthList() {
        monthOptionsList.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.US);

        for (int i = 0; i < 12; i++) {
            monthOptionsList.add(monthFmt.format(cal.getTime()));
            cal.add(Calendar.MONTH, -1);
        }
    }

    private String parseMonthPrefix(String monthYearStr) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
            Date date = inFormat.parse(monthYearStr);
            if (date != null) {
                SimpleDateFormat outFormat = new SimpleDateFormat("yyyy_MM", Locale.US);
                return outFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        if (tvSelectDate != null) {
            tvSelectDate.setText("📅   " + sdf.format(selectedDateCal.getTime()));
        }
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateCal.set(Calendar.YEAR, year);
                    selectedDateCal.set(Calendar.MONTH, month);
                    selectedDateCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateText();
                    updateReport();
                },
                selectedDateCal.get(Calendar.YEAR),
                selectedDateCal.get(Calendar.MONTH),
                selectedDateCal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateReport() {
        currentReportList.clear();
        final int activeReqId = ++reportRequestId;

        String selectedStudentFilter = spStudent.getSelectedItem() != null ? spStudent.getSelectedItem().toString() : "All Assigned Students";

        if (loadedAssignedStudents.isEmpty()) {
            if (tvPercentage != null) tvPercentage.setText("100%");
            if (tvTotalDays != null) tvTotalDays.setText("0 Days");
            if (tvPresentDays != null) tvPresentDays.setText("0 Days");
            if (tvAbsentDays != null) tvAbsentDays.setText("0 Days");
            renderReportList();
            return;
        }

        if (isMonthlyMode) {
            String selectedMonthStr = spMonth.getSelectedItem() != null ? spMonth.getSelectedItem().toString() : "";
            String monthPrefix = parseMonthPrefix(selectedMonthStr);

            if (tvReportListTitle != null) {
                tvReportListTitle.setText("Monthly Attendance Report - " + selectedMonthStr);
            }

            final int[] totalPresentSum = {0};
            final int[] totalAbsentSum = {0};

            for (StudentDto s : loadedAssignedStudents) {
                if (!selectedStudentFilter.equals("All Assigned Students") && !s.getName().equalsIgnoreCase(selectedStudentFilter)) {
                    continue;
                }

                repository.getAttendanceForStudent(s.getRegisterNumber(), new TrackPointRepository.RepositoryCallback<List<AttendanceDto>>() {
                    @Override
                    public void onSuccess(List<AttendanceDto> attendanceList) {
                        if (activeReqId != reportRequestId) return;
                        int stPresent = 0;
                        int stAbsent = 0;

                        if (attendanceList != null) {
                            for (AttendanceDto att : attendanceList) {
                                if (att.getDateKey() != null && att.getDateKey().startsWith(monthPrefix)) {
                                    if ("Present".equalsIgnoreCase(att.getStatus())) {
                                        stPresent++;
                                    } else if ("Absent".equalsIgnoreCase(att.getStatus())) {
                                        stAbsent++;
                                    }
                                }
                            }
                        }

                        int stTotal = stPresent + stAbsent;
                        totalPresentSum[0] += stPresent;
                        totalAbsentSum[0] += stAbsent;

                        int perc = stTotal > 0 ? (int) Math.round(((double) stPresent / stTotal) * 100) : 100;
                        String detailStr = stTotal > 0 ? (perc + "% (" + stPresent + "/" + stTotal + " Present)") : "Not Mentioned";

                        currentReportList.add(new StudentReportItem(s.getName(), detailStr, perc >= 75));

                        int overallTotal = totalPresentSum[0] + totalAbsentSum[0];
                        int overallPerc = overallTotal > 0 ? (int) Math.round(((double) totalPresentSum[0] / overallTotal) * 100) : 100;

                        if (tvPercentage != null) tvPercentage.setText(overallPerc + "%");
                        if (tvTotalDays != null) tvTotalDays.setText(overallTotal + " Days");
                        if (tvPresentDays != null) tvPresentDays.setText(totalPresentSum[0] + " Days");
                        if (tvAbsentDays != null) tvAbsentDays.setText(totalAbsentSum[0] + " Days");

                        renderReportList();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (activeReqId != reportRequestId) return;
                        currentReportList.add(new StudentReportItem(s.getName(), "Not Mentioned", false));
                        renderReportList();
                    }
                });
            }

        } else {
            // Specific Date Report
            SimpleDateFormat displayFmt = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.US);

            String dateDisplay = displayFmt.format(selectedDateCal.getTime());
            String dateKeyStr = keyFmt.format(selectedDateCal.getTime());

            if (tvReportListTitle != null) {
                tvReportListTitle.setText("Specific Date Report - " + dateDisplay);
            }

            final int[] totalMarkedCount = {0};
            final int[] presentCount = {0};

            for (StudentDto s : loadedAssignedStudents) {
                if (!selectedStudentFilter.equals("All Assigned Students") && !s.getName().equalsIgnoreCase(selectedStudentFilter)) {
                    continue;
                }

                repository.getAttendanceStatus(s.getRegisterNumber(), dateKeyStr, new TrackPointRepository.RepositoryCallback<AttendanceDto>() {
                    @Override
                    public void onSuccess(AttendanceDto attendance) {
                        if (activeReqId != reportRequestId) return;
                        String statusVal = attendance != null ? attendance.getStatus() : null;
                        boolean isPresent = "Present".equalsIgnoreCase(statusVal);
                        boolean isAbsent = "Absent".equalsIgnoreCase(statusVal);

                        if (isPresent) {
                            presentCount[0]++;
                            totalMarkedCount[0]++;
                            currentReportList.add(new StudentReportItem(s.getName(), "Present", true));
                        } else if (isAbsent) {
                            totalMarkedCount[0]++;
                            currentReportList.add(new StudentReportItem(s.getName(), "Absent", false));
                        } else {
                            currentReportList.add(new StudentReportItem(s.getName(), "Not Mentioned", false));
                        }

                        int perc = totalMarkedCount[0] > 0 ? (int) Math.round(((double) presentCount[0] / totalMarkedCount[0]) * 100) : 100;
                        if (tvPercentage != null) tvPercentage.setText(perc + "%");
                        if (tvTotalDays != null) tvTotalDays.setText(totalMarkedCount[0] + " Days");
                        if (tvPresentDays != null) tvPresentDays.setText(presentCount[0] + " Days");
                        if (tvAbsentDays != null) tvAbsentDays.setText((totalMarkedCount[0] - presentCount[0]) + " Days");

                        renderReportList();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (activeReqId != reportRequestId) return;
                        currentReportList.add(new StudentReportItem(s.getName(), "Not Mentioned", false));
                        renderReportList();
                    }
                });
            }
        }

        renderReportList();
    }

    private void renderReportList() {
        if (studentAttendanceList == null) return;
        studentAttendanceList.removeAllViews();

        if (currentReportList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No students assigned to " + loggedInAdminName + " in Neon database");
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(android.graphics.Color.parseColor("#68738A"));
            studentAttendanceList.addView(emptyView);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (StudentReportItem item : currentReportList) {
            View row = inflater.inflate(
                    R.layout.item_attendance_report,
                    studentAttendanceList,
                    false
            );

            TextView tvName = row.findViewById(R.id.tvReportStudentName);
            TextView tvPerc = row.findViewById(R.id.tvReportPercentage);

            if (tvName != null) tvName.setText(item.studentName);
            if (tvPerc != null) {
                tvPerc.setText(item.statusOrPercentage);
                if (item.isPresent) {
                    tvPerc.setTextColor(Color.parseColor("#10B23E"));
                } else if ("Not Mentioned".equalsIgnoreCase(item.statusOrPercentage)) {
                    tvPerc.setTextColor(Color.parseColor("#7A8294"));
                } else {
                    tvPerc.setTextColor(Color.parseColor("#D94343"));
                }
            }

            studentAttendanceList.addView(row);
        }
    }

    private void generateAndDownloadPdf() {
        String filterTypeStr = isMonthlyMode ?
                (spMonth.getSelectedItem() != null ? spMonth.getSelectedItem().toString() : "Monthly") :
                new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(selectedDateCal.getTime());

        String cleanFilterName = filterTypeStr.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = "Attendance_Report_" + loggedInAdminName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + cleanFilterName + ".pdf";

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // 1. Header Background Bar (#4935B8)
        paint.setColor(Color.parseColor("#4935B8"));
        canvas.drawRect(0, 0, 595, 75, paint);

        // 2. Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("TrackPoint - Attendance Report", 30, 45, paint);

        // 3. Subtitle Filters
        paint.setColor(Color.parseColor("#18233F"));
        paint.setTextSize(13);
        paint.setFakeBoldText(true);
        canvas.drawText("Admin: " + loggedInAdminName, 30, 105, paint);
        canvas.drawText("Filter / Period: " + filterTypeStr, 30, 125, paint);

        // 4. Summary Stats Box (#F5F6FC)
        paint.setColor(Color.parseColor("#F5F6FC"));
        canvas.drawRect(30, 145, 565, 220, paint);

        paint.setColor(Color.parseColor("#18233F"));
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        String overallPerc = tvPercentage != null ? tvPercentage.getText().toString() : "100%";
        String totalD = tvTotalDays != null ? tvTotalDays.getText().toString() : "0 Days";
        String presentD = tvPresentDays != null ? tvPresentDays.getText().toString() : "0 Days";
        String absentD = tvAbsentDays != null ? tvAbsentDays.getText().toString() : "0 Days";

        canvas.drawText("Overall Percentage: " + overallPerc, 45, 170, paint);
        canvas.drawText("Total Days: " + totalD, 45, 190, paint);
        canvas.drawText("Present Days: " + presentD + "   |   Absent Days: " + absentD, 45, 210, paint);

        // 5. Student Table Header
        paint.setColor(Color.parseColor("#4935B8"));
        paint.setTextSize(13);
        paint.setFakeBoldText(true);
        canvas.drawText("Student Name", 30, 250, paint);
        canvas.drawText("Attendance Status / Percentage", 340, 250, paint);

        paint.setColor(Color.parseColor("#EEEEF3"));
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(30, 260, 565, 260, paint);

        // 6. Student Table Rows
        int y = 285;
        paint.setColor(Color.parseColor("#18233F"));
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        for (StudentReportItem s : currentReportList) {
            if (y > 800) break; // Aligned strictly inside 1 page format
            canvas.drawText(s.studentName, 30, y, paint);

            if (s.isPresent) {
                paint.setColor(Color.parseColor("#10B23E"));
            } else if ("Not Mentioned".equalsIgnoreCase(s.statusOrPercentage)) {
                paint.setColor(Color.parseColor("#7A8294"));
            } else {
                paint.setColor(Color.parseColor("#D94343"));
            }
            canvas.drawText(s.statusOrPercentage, 340, y, paint);

            paint.setColor(Color.parseColor("#18233F"));
            y += 24;
        }

        document.finishPage(page);

        // Save PDF file directly to public Downloads folder of mobile device
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File pdfFile = new File(downloadsDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.flush();
            fos.close();

            // Notify System DownloadManager so notification appears in top bar & Downloads app
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.addCompletedDownload(
                        fileName,
                        "TrackPoint Attendance Report PDF",
                        true,
                        "application/pdf",
                        pdfFile.getAbsolutePath(),
                        pdfFile.length(),
                        true
                );
            }

            Toast.makeText(this, "📄 PDF downloaded directly to your mobile Downloads folder: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to download PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
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
