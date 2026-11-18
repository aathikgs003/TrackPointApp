package com.example.trackpoint.network.dto;

import com.google.gson.annotations.SerializedName;

public class AnnouncementDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("posted_by")
    private String postedBy;

    @SerializedName("target_role")
    private String targetRole;

    @SerializedName("created_at")
    private String createdAt;

    public AnnouncementDto() {}

    public AnnouncementDto(String title, String message, String postedBy, String targetRole) {
        this.title = title;
        this.message = message;
        this.postedBy = postedBy;
        this.targetRole = targetRole != null ? targetRole : "ALL";
        this.createdAt = "";
    }

    public AnnouncementDto(Long id, String title, String message, String postedBy, String targetRole, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.postedBy = postedBy;
        this.targetRole = targetRole;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
