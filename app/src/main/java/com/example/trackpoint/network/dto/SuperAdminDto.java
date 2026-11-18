package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class SuperAdminDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("admin_id")
    private String adminId;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("updated_at")
    private String updatedAt;

    public SuperAdminDto() {}

    public SuperAdminDto(String name, String adminId, String email, String phone, String password) {
        this.name = name;
        this.adminId = adminId;
        this.email = email;
        this.phone = phone;
        this.password = password != null ? password : "admin";
        this.updatedAt = "";
    }

    public SuperAdminDto(Long id, String name, String adminId, String email, String phone, String password, String updatedAt) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
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
