package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class StudentDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("register_number")
    private String registerNumber;

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

    @SerializedName("assigned_admin")
    private String assignedAdmin;

    @SerializedName("updated_at")
    private String updatedAt;

    public StudentDto() {}

    public StudentDto(String name, String registerNumber, String department, String year, String email, String phone, String password, String assignedAdmin) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password != null ? password : "123";
        this.assignedAdmin = assignedAdmin != null ? assignedAdmin : "Aathi";
        this.updatedAt = "";
    }

    public StudentDto(Long id, String name, String registerNumber, String department, String year, String email, String phone, String password, String assignedAdmin, String updatedAt) {
        this.id = id;
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.assignedAdmin = assignedAdmin;
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

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
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

    public String getAssignedAdmin() {
        return assignedAdmin;
    }

    public void setAssignedAdmin(String assignedAdmin) {
        this.assignedAdmin = assignedAdmin;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
