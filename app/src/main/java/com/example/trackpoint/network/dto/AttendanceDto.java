package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class AttendanceDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("register_number")
    private String registerNumber;

    @SerializedName("date_key")
    private String dateKey;

    @SerializedName("status")
    private String status;

    @SerializedName("marked_by")
    private String markedBy;

    @SerializedName("updated_at")
    private String updatedAt;

    public AttendanceDto() {}

    public AttendanceDto(String registerNumber, String dateKey, String status, String markedBy) {
        this.registerNumber = registerNumber;
        this.dateKey = dateKey;
        this.status = status;
        this.markedBy = markedBy != null ? markedBy : "Admin";
        this.updatedAt = "";
    }

    public AttendanceDto(Long id, String registerNumber, String dateKey, String status, String markedBy, String updatedAt) {
        this.id = id;
        this.registerNumber = registerNumber;
        this.dateKey = dateKey;
        this.status = status;
        this.markedBy = markedBy;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
