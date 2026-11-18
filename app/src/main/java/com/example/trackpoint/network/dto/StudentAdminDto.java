package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class StudentAdminDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("admin_id")
    private String adminId;

    @SerializedName("department")
    private String department;

    @SerializedName("year")
    private String year;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("students_assigned_count")
    private Integer studentsAssignedCount;

    @SerializedName("assigned_staff_admin")
    private String assignedStaffAdmin;

    @SerializedName("updated_at")
    private String updatedAt;

    public StudentAdminDto() {}

    public StudentAdminDto(String name, String adminId, String department, String year, String email, String phone, String password, Integer studentsAssignedCount, String assignedStaffAdmin) {
        this.name = name;
        this.adminId = adminId;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password != null ? password : "123";
        this.studentsAssignedCount = studentsAssignedCount != null ? studentsAssignedCount : 0;
        this.assignedStaffAdmin = assignedStaffAdmin != null ? assignedStaffAdmin : "Suruthika J";
        this.updatedAt = "";
    }

    public StudentAdminDto(Long id, String name, String adminId, String department, String year, String email, String phone, String password, Integer studentsAssignedCount, String assignedStaffAdmin, String updatedAt) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.studentsAssignedCount = studentsAssignedCount;
        this.assignedStaffAdmin = assignedStaffAdmin;
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

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
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

    public Integer getStudentsAssignedCount() {
        return studentsAssignedCount;
    }

    public void setStudentsAssignedCount(Integer studentsAssignedCount) {
        this.studentsAssignedCount = studentsAssignedCount;
    }

    public String getAssignedStaffAdmin() {
        return assignedStaffAdmin;
    }

    public void setAssignedStaffAdmin(String assignedStaffAdmin) {
        this.assignedStaffAdmin = assignedStaffAdmin;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
