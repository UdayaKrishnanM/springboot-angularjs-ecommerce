package com.ecommerce.demo.model;

// CHANGE 9: Added validation annotations.
// These work with @Valid in the controller to automatically reject invalid input
// before your service code even runs.
// The GlobalExceptionHandler catches MethodArgumentNotValidException and returns
// a clear JSON error with exactly which fields failed.

import com.ecommerce.demo.email.entities.ForgetPassword;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Column(unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    // Roles: "ROLE_ADMIN" or "ROLE_USER"
    private String roles;

    @OneToOne(mappedBy = "user")
    private ForgetPassword forgetPassword;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<LoginDevice> devices;

    public User() {}

    public User(Long id, String username, String password, String email, String roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public User(Long id, String username, String password, String email, String roles,
                ForgetPassword forgetPassword, List<LoginDevice> devices) {
        this(id, username, password, email, roles);
        this.forgetPassword = forgetPassword;
        this.devices = devices;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public ForgetPassword getForgetPassword() { return forgetPassword; }
    public void setForgetPassword(ForgetPassword forgetPassword) { this.forgetPassword = forgetPassword; }
    public List<LoginDevice> getDevices() { return devices; }
    public void setDevices(List<LoginDevice> devices) { this.devices = devices; }
}
