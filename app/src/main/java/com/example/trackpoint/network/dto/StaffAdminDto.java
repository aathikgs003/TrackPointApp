package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class StaffAdminDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("staff_id")
    private String staffId;

    @SerializedName("department")
    private String department;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("updated_at")
    private String updatedAt;

    public StaffAdminDto() {}

    public StaffAdminDto(String name, String staffId, String department, String email, String phone, String password) {
        this.name = name;
        this.staffId = staffId;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.password = password != null ? password : "123";
        this.updatedAt = "";
    }

    public StaffAdminDto(Long id, String name, String staffId, String department, String email, String phone, String password, String updatedAt) {
        this.id = id;
        this.name = name;
        this.staffId = staffId;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
