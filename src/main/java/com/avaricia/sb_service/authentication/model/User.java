package com.avaricia.sb_service.authentication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Parche Steve para evitar problemas con PostgreSQL
import java.util.Random;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"Users\"")

public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "\"Id\"")
    private UUID id;

    @Column(name = "\"Name\"", nullable = false, length = 200)
    private String name;

    @Column(name = "\"Email\"", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "\"PasswordHash\"", length = 500)
    private String passwordHash;

    @Column(name = "\"PhoneNumber\"", length = 50)
    private String phoneNumber;

    @Column(name = "\"CurrentBalance\"", precision = 18, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "\"TelegramId\"")
    private Long telegramId;

    @Column(name = "\"TelegramUsername\"", length = 100)
    private String telegramUsername;

    @Column(name = "\"CreatedAt\"")
    private OffsetDateTime createdAt;

    @Column(name = "\"UpdatedAt\"")
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"Provider\"", length = 50)
    private AuthProvider provider;

    @Column(name = "\"ProviderId\"", length = 200)
    private String providerId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Parche Steve para evitar problemas con PostgreSQL
    public String ramdomizePhoneNumber() {
        Random random = new Random();
        StringBuilder phoneNumber = new StringBuilder("+57 ");
        for (int i = 0; i < 10; i++) {
            phoneNumber.append(random.nextInt(10));
        }
        return phoneNumber.toString();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        // Parche Steve para evitar problemas con PostgreSQL
        if (phoneNumber == null) {
            phoneNumber = ramdomizePhoneNumber();
        }
        if (telegramUsername == null) {
            telegramUsername = ramdomizePhoneNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
