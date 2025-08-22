package com.mindfultalk.growthlab.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "super_admin")
public class SuperAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    public SuperAdmin() {
        // Generate a UUID when a new instance is created
        this.uuid = UUID.randomUUID();
    }

    public SuperAdmin(Long id, String userId, String password) {
        this();
        this.id = id;
        this.userId = userId;
        this.password = password;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "SuperAdmin [id=" + id + ", userId=" + userId + ", password=" + password + ", uuid=" + uuid + "]";
    }
}