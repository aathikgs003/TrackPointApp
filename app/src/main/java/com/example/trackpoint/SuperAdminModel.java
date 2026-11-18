package com.example.trackpoint;

public class SuperAdminModel {

    private long id;
    private String name;
    private String adminId;
    private String email;
    private String phone;
    private String password;
    private String updatedAt;

    public SuperAdminModel() {
        this.password = "admin";
        this.updatedAt = "";
    }

    public SuperAdminModel(String name, String adminId, String email, String phone) {
        this.name = name;
        this.adminId = adminId;
        this.email = email;
        this.phone = phone;
        this.password = "admin";
        this.updatedAt = "";
    }

    public SuperAdminModel(String name, String adminId, String email, String phone, String password) {
        this.name = name;
        this.adminId = adminId;
        this.email = email;
        this.phone = phone;
        this.password = password != null && !password.isEmpty() ? password : "admin";
        this.updatedAt = "";
    }

    public SuperAdminModel(long id, String name, String adminId, String email, String phone, String password, String updatedAt) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
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

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
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
