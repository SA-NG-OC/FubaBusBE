package com.example.Fuba_BE.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid")
    private Integer userId;

    @Column(name = "fullname", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phonenumber", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleid", nullable = false)
    private Role role;

    @Column(name = "status")
    @Builder.Default
    private String status = "Active";

    @Column(name = "emailverified")
    @Builder.Default
    private Boolean emailVerified = false;

    // Account lockout fields
    @Column(name = "failedloginattempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "accountlockeduntil")
    private LocalDateTime accountLockedUntil;

    @Column(name = "lastloginat")
    private LocalDateTime lastLoginAt;

    // Password reset fields
    @Column(name = "resettoken")
    private String resetToken;

    @Column(name = "resettokenexpiry")
    private LocalDateTime resetTokenExpiry;

    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void lockAccount(int lockDurationMinutes) {
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }
}