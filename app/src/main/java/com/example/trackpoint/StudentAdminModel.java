package com.example.trackpoint;

public class StudentAdminModel {

    private long id;
    private String name;
    private String adminId;
    private String department;
    private String year;
    private String email;
    private String phone;
    private String password;
    private int studentsAssignedCount;
    private String assignedStaffAdmin;
    private String updatedAt;

    public StudentAdminModel() {
        this.password = "123";
        this.studentsAssignedCount = 0;
        this.assignedStaffAdmin = "Suruthika S";
        this.updatedAt = "";
    }

    public StudentAdminModel(String name, String adminId, String department, String year, String email, String phone) {
        this.name = name;
        this.adminId = adminId;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = "123";
        this.studentsAssignedCount = 0;
        this.assignedStaffAdmin = "Suruthika S";
        this.updatedAt = "";
    }

    public StudentAdminModel(String name, String adminId, String department, String year, String email, String phone, String assignedStaffAdmin) {
        this.name = name;
        this.adminId = adminId;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = "123";
        this.studentsAssignedCount = 0;
        this.assignedStaffAdmin = assignedStaffAdmin != null && !assignedStaffAdmin.isEmpty() ? assignedStaffAdmin : "Suruthika S";
        this.updatedAt = "";
    }

    public StudentAdminModel(long id, String name, String adminId, String department, String year, String email, String phone, String password, int studentsAssignedCount, String assignedStaffAdmin, String updatedAt) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.studentsAssignedCount = studentsAssignedCount;
        this.assignedStaffAdmin = assignedStaffAdmin != null && !assignedStaffAdmin.isEmpty() ? assignedStaffAdmin : "Suruthika S";
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public int getStudentsAssignedCount() {
        return studentsAssignedCount;
    }

    public void setStudentsAssignedCount(int studentsAssignedCount) {
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
