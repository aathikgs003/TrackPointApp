package com.example.trackpoint;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentTodayActivity extends AppCompatActivity {

    ImageView btnBack;
    TextView tvCurrentDate, tvUploadTitle, tvUploadSubtitle, tvFileName;
    TextView tvSubmittedTime, tvSubmittedFileName, tvSubmittedNotes;
    LinearLayout layoutUploadBox, layoutSelectedFile;
    LinearLayout layoutUploadForm, layoutSubmittedView;
    EditText etActivityNotes;
    Button btnSubmitActivity, btnEditSubmission, btnDeleteSubmission;

    Uri selectedFileUri;
    String todayDateKey;
    String todayDateDisplay;

    TrackPointRepository repository;
    String loggedInRegNo = "CSE22CS001";
    String currentFileKey = "";
    String currentFileName = "";

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    String fileName = getFileNameFromUri(uri);

                    if (tvFileName != null) {
                        tvFileName.setText(fileName);
                    }
                    if (tvUploadTitle != null) {
                        tvUploadTitle.setText("File Selected");
                    }
                    if (tvUploadSubtitle != null) {
                        tvUploadSubtitle.setText("Tap to change file");
                    }
                    if (layoutSelectedFile != null) {
                        layoutSelectedFile.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(this, "File selected: " + fileName, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_today_activity);

        repository = TrackPointRepository.getInstance(this);

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        btnBack = findViewById(R.id.btnBack);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvUploadTitle = findViewById(R.id.tvUploadTitle);
        tvUploadSubtitle = findViewById(R.id.tvUploadSubtitle);
        tvFileName = findViewById(R.id.tvFileName);

        tvSubmittedTime = findViewById(R.id.tvSubmittedTime);
        tvSubmittedFileName = findViewById(R.id.tvSubmittedFileName);
        tvSubmittedNotes = findViewById(R.id.tvSubmittedNotes);

        layoutUploadBox = findViewById(R.id.layoutUploadBox);
        layoutSelectedFile = findViewById(R.id.layoutSelectedFile);
        layoutUploadForm = findViewById(R.id.layoutUploadForm);
        layoutSubmittedView = findViewById(R.id.layoutSubmittedView);

        etActivityNotes = findViewById(R.id.etActivityNotes);
        btnSubmitActivity = findViewById(R.id.btnSubmitActivity);
        btnEditSubmission = findViewById(R.id.btnEditSubmission);
        btnDeleteSubmission = findViewById(R.id.btnDeleteSubmission);

        btnBack.setOnClickListener(v -> finish());

        // Setup Date Keys
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());

        todayDateDisplay = sdf.format(today);
        todayDateKey = keyFmt.format(today);

        if (tvCurrentDate != null) {
            tvCurrentDate.setText(todayDateDisplay);
        }

        // Upload Box Listener
        if (layoutUploadBox != null) {
            layoutUploadBox.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        }

        // Submit Button Listener
        if (btnSubmitActivity != null) {
            btnSubmitActivity.setOnClickListener(v -> submitTodayActivity());
        }

        // Edit Button Listener
        if (btnEditSubmission != null) {
            btnEditSubmission.setOnClickListener(v -> prepareEditMode());
        }

        // Delete Button Listener
        if (btnDeleteSubmission != null) {
            btnDeleteSubmission.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        loadSubmissionState();
    }

    private void loadSubmissionState() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        // Fetch live activity from Neon PostgreSQL database
        repository.getDailyActivity(loggedInRegNo, todayDateKey, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
            @Override
            public void onSuccess(DailyActivityDto activity) {
                if (activity != null && ((activity.getFileName() != null && !activity.getFileName().isEmpty()) || (activity.getNotes() != null && !activity.getNotes().isEmpty()))) {
                    currentFileKey = activity.getFileKey() != null ? activity.getFileKey() : "";
                    currentFileName = activity.getFileName() != null ? activity.getFileName() : "Document Uploaded";

                    String submitTime = activity.getSubmitTime() != null && !activity.getSubmitTime().isEmpty() ? activity.getSubmitTime() : "Submitted Today";
                    String notes = activity.getNotes() != null ? activity.getNotes() : "No description added";

                    if (tvSubmittedFileName != null) tvSubmittedFileName.setText("📄 " + currentFileName);
                    if (tvSubmittedTime != null) tvSubmittedTime.setText(submitTime);
                    if (tvSubmittedNotes != null) tvSubmittedNotes.setText(notes.isEmpty() ? "No description added" : notes);

                    if (layoutSubmittedView != null) layoutSubmittedView.setVisibility(View.VISIBLE);
                    if (layoutUploadForm != null) layoutUploadForm.setVisibility(View.GONE);
                } else {
                    showFormMode();
                }
            }

            @Override
            public void onError(Exception e) {
                showFormMode();
            }
        });
    }

    private void showFormMode() {
        if (layoutSubmittedView != null) layoutSubmittedView.setVisibility(View.GONE);
        if (layoutUploadForm != null) layoutUploadForm.setVisibility(View.VISIBLE);
    }

    private void submitTodayActivity() {
        String notes = etActivityNotes != null ? etActivityNotes.getText().toString().trim() : "";

        if (selectedFileUri == null && notes.isEmpty()) {
            Toast.makeText(this, "Please upload a file or enter notes before submitting", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = "Submitted at " + timeFmt.format(new Date());

        if (btnSubmitActivity != null) btnSubmitActivity.setEnabled(false);

        // Submit via TrackPointRepository (uploads file to B2 S3 and saves record to Neon)
        repository.submitDailyActivity(this, selectedFileUri, loggedInRegNo, todayDateKey, notes, timeStr, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
            @Override
            public void onSuccess(DailyActivityDto result) {
                if (btnSubmitActivity != null) btnSubmitActivity.setEnabled(true);
                Toast.makeText(StudentTodayActivity.this, "Today's activity saved to Neon PostgreSQL & Backblaze B2 S3!", Toast.LENGTH_LONG).show();
                selectedFileUri = null;
                if (etActivityNotes != null) etActivityNotes.setText("");
                if (layoutSelectedFile != null) layoutSelectedFile.setVisibility(View.GONE);
                loadSubmissionState();
            }

            @Override
            public void onError(Exception e) {
                if (btnSubmitActivity != null) btnSubmitActivity.setEnabled(true);
                Toast.makeText(StudentTodayActivity.this, "Failed to submit activity: " + (e != null ? e.getMessage() : "Network error"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareEditMode() {
        if (etActivityNotes != null && tvSubmittedNotes != null) {
            etActivityNotes.setText(tvSubmittedNotes.getText().toString().replaceAll("^No description added$", ""));
        }

        if (tvFileName != null && !currentFileName.isEmpty()) {
            tvFileName.setText(currentFileName);
            if (layoutSelectedFile != null) layoutSelectedFile.setVisibility(View.VISIBLE);
        }

        if (btnSubmitActivity != null) {
            btnSubmitActivity.setText("Update Activity");
        }

        if (layoutSubmittedView != null) layoutSubmittedView.setVisibility(View.GONE);
        if (layoutUploadForm != null) layoutUploadForm.setVisibility(View.VISIBLE);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Activity")
                .setMessage("Are you sure you want to delete today's activity submission from Neon PostgreSQL & Backblaze B2?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteTodayActivity())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteTodayActivity() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        loggedInRegNo = sessionPrefs.getString("logged_in_reg_no", "CSE22CS001");

        if (btnDeleteSubmission != null) btnDeleteSubmission.setEnabled(false);

        repository.deleteDailyActivity(loggedInRegNo, todayDateKey, currentFileKey, new TrackPointRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (btnDeleteSubmission != null) btnDeleteSubmission.setEnabled(true);
                selectedFileUri = null;
                currentFileKey = "";
                currentFileName = "";
                if (etActivityNotes != null) etActivityNotes.setText("");
                if (layoutSelectedFile != null) layoutSelectedFile.setVisibility(View.GONE);
                if (tvUploadTitle != null) tvUploadTitle.setText("Click here to upload file");
                if (tvUploadSubtitle != null) tvUploadSubtitle.setText("Supports images, PDF, Word & documents");
                if (btnSubmitActivity != null) btnSubmitActivity.setText("Submit Activity");

                Toast.makeText(StudentTodayActivity.this, "Today's activity deleted from Neon database!", Toast.LENGTH_SHORT).show();
                loadSubmissionState();
            }

            @Override
            public void onError(Exception e) {
                if (btnDeleteSubmission != null) btnDeleteSubmission.setEnabled(true);
                Toast.makeText(StudentTodayActivity.this, "Failed to delete activity: " + (e != null ? e.getMessage() : "Error"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri != null && uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null && uri != null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "Selected File";
    }
}
