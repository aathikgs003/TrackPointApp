package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class DailyActivityDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("register_number")
    private String registerNumber;

    @SerializedName("date_key")
    private String dateKey;

    @SerializedName("file_name")
    private String fileName;

    @SerializedName("file_key")
    private String fileKey; // Backblaze B2 S3 Object Key (e.g., submissions/CSE22CS001/2025-03-30_report.pdf)

    @SerializedName("file_url")
    private String fileUrl; // Presigned viewing URL

    @SerializedName("notes")
    private String notes;

    @SerializedName("submit_time")
    private String submitTime;

    @SerializedName("status")
    private String status;

    @SerializedName("staff_remark")
    private String staffRemark;

    @SerializedName("updated_at")
    private String updatedAt;

    public DailyActivityDto() {}

    public DailyActivityDto(String registerNumber, String dateKey, String fileName, String fileKey, String fileUrl, String notes, String submitTime) {
        this.registerNumber = registerNumber;
        this.dateKey = dateKey;
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.fileUrl = fileUrl;
        this.notes = notes;
        this.submitTime = submitTime;
        this.status = "Submitted";
        this.staffRemark = "";
        this.updatedAt = "";
    }

    public DailyActivityDto(Long id, String registerNumber, String dateKey, String fileName, String fileKey, String fileUrl, String notes, String submitTime, String status, String staffRemark, String updatedAt) {
        this.id = id;
        this.registerNumber = registerNumber;
        this.dateKey = dateKey;
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.fileUrl = fileUrl;
        this.notes = notes;
        this.submitTime = submitTime;
        this.status = status;
        this.staffRemark = staffRemark;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(String submitTime) {
        this.submitTime = submitTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStaffRemark() {
        return staffRemark;
    }

    public void setStaffRemark(String staffRemark) {
        this.staffRemark = staffRemark;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
