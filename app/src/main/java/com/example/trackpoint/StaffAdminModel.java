package com.example.trackpoint;

public class StaffAdminModel {

    private long id;
    private String name;
    private String staffId;
    private String department;
    private String email;
    private String phone;
    private String password;
    private String updatedAt;

    public StaffAdminModel() {
        this.password = "123";
        this.updatedAt = "";
    }

    public StaffAdminModel(String name, String staffId, String department, String email, String phone) {
        this.name = name;
        this.staffId = staffId;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.password = "123";
        this.updatedAt = "";
    }

    public StaffAdminModel(long id, String name, String staffId, String department, String email, String phone, String password, String updatedAt) {
        this.id = id;
        this.name = name;
        this.staffId = staffId;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.password = password;
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
