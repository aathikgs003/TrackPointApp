package com.example.trackpoint;

public class DailyActivityModel {

    private long id;
    private String studentRegNo;
    private String dateKey;
    private String fileName;
    private String fileUrl;
    private String notes;
    private String submitTime;
    private String status;
    private String staffRemark;
    private String updatedAt;

    public DailyActivityModel() {
        this.fileUrl = "";
        this.status = "Submitted";
        this.staffRemark = "";
        this.updatedAt = "";
    }

    public DailyActivityModel(String studentRegNo, String dateKey, String fileName, String fileUrl, String notes, String submitTime) {
        this.studentRegNo = studentRegNo;
        this.dateKey = dateKey;
        this.fileName = fileName;
        this.fileUrl = fileUrl != null ? fileUrl : "";
        this.notes = notes;
        this.submitTime = submitTime;
        this.status = "Submitted";
        this.staffRemark = "";
        this.updatedAt = "";
    }

    public DailyActivityModel(long id, String studentRegNo, String dateKey, String fileName, String fileUrl, String notes, String submitTime, String status, String staffRemark, String updatedAt) {
        this.id = id;
        this.studentRegNo = studentRegNo;
        this.dateKey = dateKey;
        this.fileName = fileName;
        this.fileUrl = fileUrl != null ? fileUrl : "";
        this.notes = notes;
        this.submitTime = submitTime;
        this.status = status;
        this.staffRemark = staffRemark;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStudentRegNo() {
        return studentRegNo;
    }

    public void setStudentRegNo(String studentRegNo) {
        this.studentRegNo = studentRegNo;
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
