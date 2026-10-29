package com.ecommerce.demo.service;

// CHANGE 10: Unit tests for UserService.
// Uses Mockito to mock dependencies — the test runs without a DB or mail server.
//
// Key patterns to understand:
//   @Mock        — creates a fake version of a dependency
//   @InjectMocks — injects the @Mock objects into the class being tested
//   when().thenReturn() — tells a mock what to return
//   verify()     — checks a method was called
//   assertThrows() — confirms an exception was thrown

import com.ecommerce.demo.dto.JwtAuthResponse;
import com.ecommerce.demo.dto.LoginRequest;
import com.ecommerce.demo.model.LoginDevice;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.repository.LoginDeviceRepository;
import com.ecommerce.demo.repository.UserRepository;
import com.ecommerce.demo.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LoginDeviceRepository deviceRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserInfoUserDetailsService userDetailsService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testuser", "encodedPassword", "test@example.com", "ROLE_USER");
        testUser.setDevices(new ArrayList<>());
    }

    // ── Registration Tests ────────────────────────────────

    @Test
    @DisplayName("Register: success when email is new")
    void registerNewUser_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encodedPassword");

        String result = userService.registerNewUserAccount(testUser);

        assertThat(result).isEqualTo("Registration successful");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Register: fail when email already exists")
    void registerNewUser_emailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        String result = userService.registerNewUserAccount(testUser);

        assertThat(result).isEqualTo("Email already registered");
        verify(userRepository, never()).save(any());
    }

    // ── Login Tests ───────────────────────────────────────

    @Test
    @DisplayName("Login: returns JWT tokens on success")
    void login_returnsJwtTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtils.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        JwtAuthResponse response = userService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("Login: throws BadCredentialsException on wrong password")
    void login_wrongPassword_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> userService.login(request));
    }

    // ── Device Tests ──────────────────────────────────────

    @Test
    @DisplayName("Device login: success when under device limit")
    void loginDevice_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String result = userService.loginDevice("test@example.com", "device-abc", "Hyderabad", "Chrome");

        assertThat(result).isEqualTo("Device logged in successfully.");
        verify(deviceRepository).save(any(LoginDevice.class));
    }

    @Test
    @DisplayName("Device login: blocked when 2 devices already active")
    void loginDevice_twoDevicesAlreadyLoggedIn() {
        LoginDevice d1 = new LoginDevice("d1", "Location1", true, "Phone", testUser);
        LoginDevice d2 = new LoginDevice("d2", "Location2", true, "Laptop", testUser);
        testUser.setDevices(List.of(d1, d2));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String result = userService.loginDevice("test@example.com", "d3", "Location3", "Tablet");

        assertThat(result).isEqualTo("Already 2 devices logged in, remove one device to login.");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Device login: returns already logged in when same device")
    void loginDevice_sameDeviceAlreadyLoggedIn() {
        LoginDevice d1 = new LoginDevice("device-abc", "Hyderabad", true, "Chrome", testUser);
        testUser.setDevices(List.of(d1));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String result = userService.loginDevice("test@example.com", "device-abc", "Hyderabad", "Chrome");

        assertThat(result).isEqualTo("Device already logged in.");
    }

    // ── Delete Tests ──────────────────────────────────────

    @Test
    @DisplayName("Delete: fails with wrong password")
    void deleteUser_wrongPassword() {
        testUser.setPassword("encodedPassword");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(encoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        User deleteRequest = new User();
        deleteRequest.setEmail("test@example.com");
        deleteRequest.setPassword("wrongPassword");

        String result = userService.delete(deleteRequest);
        assertThat(result).isEqualTo("Password incorrect");
        verify(userRepository, never()).deleteById(any());
    }
}
