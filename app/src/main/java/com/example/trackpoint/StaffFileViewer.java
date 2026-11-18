package com.example.trackpoint;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StaffFileViewer extends AppCompatActivity {

    ImageView btnBack, btnViewerDownload, imgFilePreview;
    WebView webViewPdfPreview;
    ProgressBar progressBarLoading;
    LinearLayout layoutNotesFallback;
    TextView txtViewerFileName, txtViewerStudent, txtDocHeader, txtDocBody, txtDocFooter;

    TrackPointRepository repository;
    String currentPresignedUrl = "";
    String currentFileName = "daily_record.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#4935B8"));
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_staff_file_viewer);

        repository = TrackPointRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBack);
        btnViewerDownload = findViewById(R.id.btnViewerDownload);
        txtViewerFileName = findViewById(R.id.txtViewerFileName);
        txtViewerStudent = findViewById(R.id.txtViewerStudent);

        progressBarLoading = findViewById(R.id.progressBarLoading);
        imgFilePreview = findViewById(R.id.imgFilePreview);
        webViewPdfPreview = findViewById(R.id.webViewPdfPreview);
        layoutNotesFallback = findViewById(R.id.layoutNotesFallback);

        txtDocHeader = findViewById(R.id.txtDocHeader);
        txtDocBody = findViewById(R.id.txtDocBody);
        txtDocFooter = findViewById(R.id.txtDocFooter);

        btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String studentName = intent != null ? intent.getStringExtra("studentName") : "";
        String regId = intent != null ? intent.getStringExtra("regId") : "";
        String fileType = intent != null ? intent.getStringExtra("fileType") : "PDF";
        String fileName = intent != null ? intent.getStringExtra("fileName") : "daily_record.pdf";
        String notes = intent != null ? intent.getStringExtra("notes") : "";
        String dateKey = intent != null ? intent.getStringExtra("dateKey") : "";

        if (fileName != null && !fileName.isEmpty()) {
            currentFileName = fileName;
        }

        if (dateKey == null || dateKey.isEmpty()) {
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
            dateKey = keyFmt.format(new Date());
        }

        final String activeDateKey = dateKey;
        final String activeRegId = regId != null && !regId.isEmpty() ? regId : "CSE22CS001";
        final String activeFileType = fileType != null ? fileType : "PDF";

        if (txtViewerFileName != null) txtViewerFileName.setText(currentFileName);
        if (txtViewerStudent != null) txtViewerStudent.setText("Student: " + (studentName != null ? studentName : "") + " (" + activeRegId + ")");

        // Load Student Name from Neon database if available
        repository.getStudentByRegNo(activeRegId, new TrackPointRepository.RepositoryCallback<StudentDto>() {
            @Override
            public void onSuccess(StudentDto student) {
                if (student != null && txtViewerStudent != null) {
                    txtViewerStudent.setText("Student: " + student.getName() + " (" + student.getRegisterNumber() + ")");
                }
            }

            @Override
            public void onError(Exception e) {
                // Keep default intent string
            }
        });

        // Query Daily Activity from Neon PostgreSQL database table daily_activities
        repository.getDailyActivity(activeRegId, activeDateKey, new TrackPointRepository.RepositoryCallback<DailyActivityDto>() {
            @Override
            public void onSuccess(DailyActivityDto activity) {
                if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);

                if (activity != null && activity.getFileUrl() != null && !activity.getFileUrl().isEmpty()) {
                    currentPresignedUrl = activity.getFileUrl();
                    if (activity.getFileName() != null && !activity.getFileName().isEmpty()) {
                        currentFileName = activity.getFileName();
                        if (txtViewerFileName != null) txtViewerFileName.setText(currentFileName);
                    }

                    renderFileContent(currentFileName, activeFileType, currentPresignedUrl, activity.getNotes());
                } else {
                    renderFallbackNotes(currentFileName, activeFileType, notes);
                }
            }

            @Override
            public void onError(Exception e) {
                if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);

                // Fallback to S3 presigned URL generated directly from object key
                String objectKey = "submissions/" + activeRegId + "/" + activeDateKey + "_" + currentFileName;
                currentPresignedUrl = S3StorageManager.getInstance(StaffFileViewer.this).generatePresignedUrl(objectKey, 60);

                if (currentPresignedUrl != null && !currentPresignedUrl.isEmpty()) {
                    renderFileContent(currentFileName, activeFileType, currentPresignedUrl, notes);
                } else {
                    renderFallbackNotes(currentFileName, activeFileType, notes);
                }
            }
        });

        if (btnViewerDownload != null) {
            btnViewerDownload.setOnClickListener(v -> downloadFileDirectlyToDevice(currentPresignedUrl, currentFileName));
        }
    }

    private void downloadFileDirectlyToDevice(String fileUrl, String fileName) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            Toast.makeText(this, "File URL unavailable for download", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle("Downloading " + fileName);
            request.setDescription("Downloading file from TrackPoint Storage...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(this, "Downloading " + fileName + " directly to your Downloads folder...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Download Manager unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderFileContent(String fileName, String fileType, String url, String notes) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        boolean isImage = lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") || "Image".equalsIgnoreCase(fileType) || "IMG".equalsIgnoreCase(fileType);

        if (isImage) {
            if (imgFilePreview != null) {
                imgFilePreview.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.action_card)
                        .into(imgFilePreview);
            }
            if (webViewPdfPreview != null) webViewPdfPreview.setVisibility(View.GONE);
            if (layoutNotesFallback != null) layoutNotesFallback.setVisibility(View.GONE);
        } else {
            // PDF or Document Web Viewer
            if (webViewPdfPreview != null) {
                webViewPdfPreview.setVisibility(View.VISIBLE);
                if (imgFilePreview != null) imgFilePreview.setVisibility(View.GONE);
                if (layoutNotesFallback != null) layoutNotesFallback.setVisibility(View.GONE);

                try {
                    WebSettings settings = webViewPdfPreview.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setBuiltInZoomControls(true);
                    settings.setDisplayZoomControls(false);

                    webViewPdfPreview.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String finishedUrl) {
                            if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);
                        }
                    });

                    String googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(url, "UTF-8");
                    webViewPdfPreview.loadUrl(googleDocsUrl);
                } catch (Exception e) {
                    renderFallbackNotes(fileName, fileType, notes);
                }
            } else {
                renderFallbackNotes(fileName, fileType, notes);
            }
        }
    }

    private void renderFallbackNotes(String fileName, String fileType, String notes) {
        if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);
        if (imgFilePreview != null) imgFilePreview.setVisibility(View.GONE);
        if (webViewPdfPreview != null) webViewPdfPreview.setVisibility(View.GONE);

        if (layoutNotesFallback != null) {
            layoutNotesFallback.setVisibility(View.VISIBLE);
            if (txtDocHeader != null) txtDocHeader.setText("DOCUMENT SUBMISSION SUMMARY");
            if (txtDocBody != null) txtDocBody.setText("File: " + fileName + "\n\nNotes:\n" + (notes != null && !notes.isEmpty() ? notes : "No description provided") + "\n\nStatus: Logged in Neon PostgreSQL & Backblaze B2 S3 Storage.");
            if (txtDocFooter != null) txtDocFooter.setText("Document Signed • Backblaze B2 Active");
        }
    }
}
