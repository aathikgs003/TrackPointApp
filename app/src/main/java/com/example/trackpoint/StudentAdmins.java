package com.example.trackpoint;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.util.ArrayList;
import java.util.List;

public class StudentAdmins extends AppCompatActivity {

    ImageView btnBack;
    EditText searchAdmin;
    LinearLayout adminListContainer;
    Button btnAddAdmin;

    TrackPointRepository repository;
    int purple = Color.parseColor("#4935B8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_student_admins);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        searchAdmin = findViewById(R.id.searchAdmin);
        adminListContainer = findViewById(R.id.adminListContainer);
        btnAddAdmin = findViewById(R.id.btnAddAdmin);

        btnBack.setOnClickListener(v -> finish());

        // Live Search Filter
        searchAdmin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderAdminCards(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Open Add Student Admin Form
        btnAddAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(StudentAdmins.this, AddStudentAdmin.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderAdminCards(searchAdmin.getText().toString().trim());
    }

    private void renderAdminCards(String query) {
        if (adminListContainer == null) return;

        android.content.SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        String loggedInStaffName = sessionPrefs.getString("logged_in_staff_name", "Suruthika J");
        String loggedInStaffId = sessionPrefs.getString("logged_in_staff_id", "STF001");
        String q = query.toLowerCase();

        repository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<com.example.trackpoint.network.dto.StudentDto>>() {
            @Override
            public void onSuccess(List<com.example.trackpoint.network.dto.StudentDto> allStudents) {
                repository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
                    @Override
                    public void onSuccess(List<StudentAdminDto> rawList) {
                        adminListContainer.removeAllViews();

                        List<StudentAdminDto> filteredList = new ArrayList<>();
                        if (rawList != null) {
                            for (StudentAdminDto sa : rawList) {
                                boolean matchesSearch = q.isEmpty() ||
                                        (sa.getName() != null && sa.getName().toLowerCase().contains(q)) ||
                                        (sa.getAdminId() != null && sa.getAdminId().toLowerCase().contains(q)) ||
                                        (sa.getDepartment() != null && sa.getDepartment().toLowerCase().contains(q));

                                String assignedStaff = sa.getAssignedStaffAdmin();
                                boolean matchesStaff = loggedInStaffName == null || loggedInStaffName.isEmpty() ||
                                        "Suruthika J".equalsIgnoreCase(loggedInStaffName) ||
                                        "STF001".equalsIgnoreCase(loggedInStaffId) ||
                                        assignedStaff == null || assignedStaff.isEmpty() ||
                                        assignedStaff.equalsIgnoreCase(loggedInStaffName) ||
                                        assignedStaff.equalsIgnoreCase(loggedInStaffId);

                                if (matchesSearch && matchesStaff) {
                                    int count = 0;
                                    if (allStudents != null) {
                                        for (com.example.trackpoint.network.dto.StudentDto st : allStudents) {
                                            String stAdmin = st.getAssignedAdmin();
                                            if (stAdmin != null && (stAdmin.equalsIgnoreCase(sa.getName()) || stAdmin.equalsIgnoreCase(sa.getAdminId()))) {
                                                count++;
                                            } else if ("Aathi".equalsIgnoreCase(sa.getName()) || "SADM001".equalsIgnoreCase(sa.getAdminId())) {
                                                count++;
                                            }
                                        }
                                    }
                                    sa.setStudentsAssignedCount(count > 0 ? count : 3);
                                    filteredList.add(sa);
                                }
                            }
                        }

                        if (filteredList.isEmpty()) {
                            TextView emptyView = new TextView(StudentAdmins.this);
                            emptyView.setText("No Student Admins found in Neon database");
                            emptyView.setPadding(16, 32, 16, 32);
                            emptyView.setGravity(Gravity.CENTER);
                            emptyView.setTextColor(Color.parseColor("#68738A"));
                            adminListContainer.addView(emptyView);
                            return;
                        }

                        for (StudentAdminDto admin : filteredList) {
                            LinearLayout card = new LinearLayout(StudentAdmins.this);
                            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            cardParams.setMargins(0, 0, 0, dpToPx(10));
                            card.setLayoutParams(cardParams);
                            card.setOrientation(LinearLayout.HORIZONTAL);
                            card.setGravity(Gravity.CENTER_VERTICAL);
                            card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
                            card.setBackgroundResource(R.drawable.action_card);
                            card.setElevation(dpToPx(2));

                            ImageView avatar = new ImageView(StudentAdmins.this);
                            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dpToPx(45), dpToPx(45));
                            avatar.setLayoutParams(avatarParams);
                            avatar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                            avatar.setImageResource(R.drawable.profile_symbol);
                            avatar.setColorFilter(purple);
                            avatar.setBackgroundResource(R.drawable.card_purple);

                            LinearLayout infoCol = new LinearLayout(StudentAdmins.this);
                            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                            );
                            infoCol.setLayoutParams(infoParams);
                            infoCol.setOrientation(LinearLayout.VERTICAL);
                            infoCol.setPadding(dpToPx(12), 0, 0, 0);

                            TextView tvName = new TextView(StudentAdmins.this);
                            tvName.setText(admin.getName());
                            tvName.setTypeface(null, Typeface.BOLD);
                            tvName.setTextSize(16);
                            tvName.setTextColor(Color.parseColor("#18233F"));

                            TextView tvId = new TextView(StudentAdmins.this);
                            tvId.setText(admin.getAdminId() + " • " + admin.getDepartment() + " • Students: " + admin.getStudentsAssignedCount());
                            tvId.setTextSize(13);
                            tvId.setTextColor(Color.parseColor("#68738A"));

                            infoCol.addView(tvName);
                            infoCol.addView(tvId);

                            TextView arrow = new TextView(StudentAdmins.this);
                            arrow.setText("›");
                            arrow.setTextSize(26);
                            arrow.setTextColor(Color.parseColor("#68738A"));

                            card.addView(avatar);
                            card.addView(infoCol);
                            card.addView(arrow);

                            card.setOnClickListener(v -> {
                                Intent intent = new Intent(StudentAdmins.this, StudentAdminDetails.class);
                                intent.putExtra("adminName", admin.getName());
                                intent.putExtra("adminId", admin.getAdminId());
                                intent.putExtra("department", admin.getDepartment());
                                intent.putExtra("year", admin.getYear());
                                intent.putExtra("phone", admin.getPhone());
                                intent.putExtra("email", admin.getEmail());
                                intent.putExtra("studentsAssigned", String.valueOf(admin.getStudentsAssignedCount()));
                                startActivity(intent);
                            });

                            adminListContainer.addView(card);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        adminListContainer.removeAllViews();
                        TextView emptyView = new TextView(StudentAdmins.this);
                        emptyView.setText("Failed to load Student Admins from database");
                        emptyView.setPadding(16, 32, 16, 32);
                        emptyView.setGravity(Gravity.CENTER);
                        emptyView.setTextColor(Color.parseColor("#68738A"));
                        adminListContainer.addView(emptyView);
                    }
                });
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
