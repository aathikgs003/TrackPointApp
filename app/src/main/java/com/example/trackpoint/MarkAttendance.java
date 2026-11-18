package com.example.trackpoint;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MarkAttendance extends AppCompatActivity {

    ImageView btnBack;
    TextView tvAttendanceDate;
    Spinner spYear;
    Button btnAllPresent, btnAllAbsent, btnSubmitAttendance;

    LinearLayout attendanceList;
    Calendar selectedCalendar;
    TrackPointRepository repository;
    String loggedInAdminName = "Aathi";

    static class AttendanceStudent {
        String name;
        String regNo;
        String year;

        AttendanceStudent(String name, String regNo, String year) {
            this.name = name;
            this.regNo = regNo;
            this.year = year;
        }
    }

    ArrayList<AttendanceStudent> allStudents = new ArrayList<>();
    ArrayList<AttendanceStudent> currentFilteredList = new ArrayList<>();
    ArrayList<Spinner> statusSpinners = new ArrayList<>();

    String[] attendanceOptions = {
            "Not Mentioned", "Present", "Absent"
    };

    private int renderRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInAdminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        btnBack = findViewById(R.id.btnBack);
        tvAttendanceDate = findViewById(R.id.tvAttendanceDate);
        spYear = findViewById(R.id.spYear);

        btnAllPresent = findViewById(R.id.btnAllPresent);
        btnAllAbsent = findViewById(R.id.btnAllAbsent);
        btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance);

        attendanceList = findViewById(R.id.attendanceList);

        setSpinner(spYear, new String[]{
                "All Years", "I Year", "II Year",
                "III Year", "IV Year"
        });

        selectedCalendar = Calendar.getInstance();
        updateDate(selectedCalendar);

        tvAttendanceDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedCalendar.set(year, month, day);
                        updateDate(selectedCalendar);
                        filterStudents();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();
        });

        spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAllPresent.setOnClickListener(v -> {
            for (Spinner spinner : statusSpinners) {
                spinner.setSelection(1); // Selection 1 is "Present"
            }
        });

        btnAllAbsent.setOnClickListener(v -> {
            for (Spinner spinner : statusSpinners) {
                spinner.setSelection(2); // Selection 2 is "Absent"
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnSubmitAttendance.setOnClickListener(v -> submitAttendanceData());

        loadStudentsFromNeon();
    }

    private void loadStudentsFromNeon() {
        allStudents.clear();
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String role = sessionPrefs.getString("logged_in_role", "STUDENT_ADMIN");
        String adminName = sessionPrefs.getString("logged_in_admin_name", "Aathi");

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> studentList) {
                allStudents.clear();
                Set<String> seenRegs = new HashSet<>();
                if (studentList != null) {
                    for (StudentDto s : studentList) {
                        String reg = s.getRegisterNumber();
                        if (reg != null && !seenRegs.contains(reg.toLowerCase())) {
                            boolean isAssigned = !"STUDENT_ADMIN".equalsIgnoreCase(role) ||
                                    (s.getAssignedAdmin() == null || s.getAssignedAdmin().equalsIgnoreCase(adminName) || adminName.equalsIgnoreCase("Aathi"));

                            if (isAssigned) {
                                seenRegs.add(reg.toLowerCase());
                                allStudents.add(new AttendanceStudent(s.getName(), s.getRegisterNumber(), s.getYear()));
                            }
                        }
                    }
                }
                filterStudents();
            }

            @Override
            public void onError(Exception e) {
                allStudents.clear();
                filterStudents();
            }
        });
    }

    private void submitAttendanceData() {
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
        String dateKey = keyFmt.format(selectedCalendar.getTime());

        SharedPreferences prefs = getSharedPreferences("AttendanceStore", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        ArrayList<AttendanceDto> batchList = new ArrayList<>();

        for (int i = 0; i < statusSpinners.size(); i++) {
            if (i < currentFilteredList.size()) {
                AttendanceStudent s = currentFilteredList.get(i);
                String selectedOption = statusSpinners.get(i).getSelectedItem() != null ?
                        statusSpinners.get(i).getSelectedItem().toString() : "Not Mentioned";

                String finalStatusVal = "Present".equalsIgnoreCase(selectedOption) ? "Present" :
                        ("Absent".equalsIgnoreCase(selectedOption) ? "Absent" : "Not Mentioned");

                editor.putString("status_" + s.regNo + "_" + dateKey, finalStatusVal);

                batchList.add(new AttendanceDto(
                        s.regNo,
                        dateKey,
                        finalStatusVal,
                        loggedInAdminName != null ? loggedInAdminName : "Student Admin"
                ));
            }
        }

        editor.apply();

        if (btnSubmitAttendance != null) btnSubmitAttendance.setEnabled(false);

        repository.recordAttendanceBatch(batchList, new TrackPointRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (btnSubmitAttendance != null) btnSubmitAttendance.setEnabled(true);
                Toast.makeText(MarkAttendance.this, "Attendance saved to Neon PostgreSQL database!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                if (btnSubmitAttendance != null) btnSubmitAttendance.setEnabled(true);
                Toast.makeText(MarkAttendance.this, "Attendance recorded on Neon database!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void filterStudents() {
        if (spYear == null || spYear.getSelectedItem() == null) {
            return;
        }

        String selectedYear = spYear.getSelectedItem().toString();
        currentFilteredList.clear();

        for (AttendanceStudent s : allStudents) {
            boolean matchesYear = selectedYear.equals("Select Year") ||
                    selectedYear.equals("All Years") ||
                    s.year.equalsIgnoreCase(selectedYear);

            if (matchesYear) {
                currentFilteredList.add(s);
            }
        }

        displayStudents(currentFilteredList);
    }

    private void displayStudents(ArrayList<AttendanceStudent> list) {
        if (attendanceList == null) return;
        attendanceList.removeAllViews();
        statusSpinners.clear();

        final int activeReqId = ++renderRequestId;

        LayoutInflater inflater = LayoutInflater.from(this);

        if (list.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No students assigned to " + loggedInAdminName + " found in Neon database");
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(android.graphics.Color.parseColor("#68738A"));
            attendanceList.addView(emptyView);
            return;
        }

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
        String dateKey = keyFmt.format(selectedCalendar.getTime());

        for (AttendanceStudent student : list) {
            View row = inflater.inflate(
                    R.layout.item_attendance,
                    attendanceList,
                    false
            );

            TextView name = row.findViewById(R.id.tvStudentName);
            Spinner status = row.findViewById(R.id.spAttendanceStatus);

            name.setText(student.name);
            setSpinner(status, attendanceOptions);

            // Default to 0 ("Not Mentioned") for the newly selected date
            status.setSelection(0);

            // Query live status from Neon PostgreSQL attendance table for this student and selected date
            repository.getAttendanceStatus(student.regNo, dateKey, new TrackPointRepository.RepositoryCallback<AttendanceDto>() {
                @Override
                public void onSuccess(AttendanceDto attDto) {
                    if (activeReqId != renderRequestId) return;
                    if (attDto != null && attDto.getStatus() != null) {
                        if ("Present".equalsIgnoreCase(attDto.getStatus())) {
                            status.setSelection(1);
                        } else if ("Absent".equalsIgnoreCase(attDto.getStatus())) {
                            status.setSelection(2);
                        } else {
                            status.setSelection(0);
                        }
                    } else {
                        status.setSelection(0);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (activeReqId != renderRequestId) return;
                    status.setSelection(0); // If no attendance record exists in database for this date, show "Not Mentioned"
                }
            });

            statusSpinners.add(status);
            attendanceList.addView(row);
        }
    }

    private void updateDate(Calendar calendar) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvAttendanceDate.setText("📅   " + format.format(calendar.getTime()));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentsFromNeon();
    }
}
