package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class DepartmentDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("code")
    private String code;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("updated_at")
    private String updatedAt;

    public DepartmentDto() {}

    public DepartmentDto(String code, String fullName) {
        this.code = code;
        this.fullName = fullName;
        this.updatedAt = "";
    }

    public DepartmentDto(Long id, String code, String fullName, String updatedAt) {
        this.id = id;
        this.code = code;
        this.fullName = fullName;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
