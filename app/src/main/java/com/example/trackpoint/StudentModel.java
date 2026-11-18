package com.example.trackpoint;

public class StudentModel {

    private long id;
    private String name;
    private String registerNumber;
    private String department;
    private String year;
    private String email;
    private String phone;
    private String password;
    private String assignedAdmin;
    private String serverId;
    private boolean isSynced;
    private String updatedAt;

    public StudentModel() {
        this.password = "123";
        this.assignedAdmin = "Aathi";
        this.serverId = "";
        this.isSynced = false;
        this.updatedAt = "";
    }

    public StudentModel(String name, String registerNumber, String department, String year, String email, String phone) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = "123";
        this.assignedAdmin = "Aathi";
        this.serverId = "";
        this.isSynced = false;
        this.updatedAt = "";
    }

    public StudentModel(String name, String registerNumber, String department, String year, String email, String phone, String password) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password != null && !password.isEmpty() ? password : "123";
        this.assignedAdmin = "Aathi";
        this.serverId = "";
        this.isSynced = false;
        this.updatedAt = "";
    }

    public StudentModel(String name, String registerNumber, String department, String year, String email, String phone, String password, String assignedAdmin) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password != null && !password.isEmpty() ? password : "123";
        this.assignedAdmin = assignedAdmin != null ? assignedAdmin : "Aathi";
        this.serverId = "";
        this.isSynced = false;
        this.updatedAt = "";
    }

    public StudentModel(long id, String name, String registerNumber, String department, String year, String email, String phone, String password, String assignedAdmin, String serverId, boolean isSynced, String updatedAt) {
        this.id = id;
        this.name = name;
        this.registerNumber = registerNumber;
        this.department = department;
        this.year = year;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.assignedAdmin = assignedAdmin != null ? assignedAdmin : "Aathi";
        this.serverId = serverId;
        this.isSynced = isSynced;
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

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
