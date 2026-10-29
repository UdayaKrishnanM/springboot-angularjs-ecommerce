package com.ecommerce.demo.service;

// CHANGE 1: Fixed == string comparison bug (used .equals() throughout)
// CHANGE 4: Login now returns JWT tokens instead of plain string
// CHANGE 10: Input validation added

import com.ecommerce.demo.dto.JwtAuthResponse;
import com.ecommerce.demo.dto.LoginRequest;
import com.ecommerce.demo.dto.UserDTO;
import com.ecommerce.demo.exception.DeviceNotFoundException;
import com.ecommerce.demo.exception.UserNameNotFoundException;
import com.ecommerce.demo.model.LoginDevice;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.repository.LoginDeviceRepository;
import com.ecommerce.demo.repository.UserRepository;
import com.ecommerce.demo.security.JwtUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private LoginDeviceRepository deviceRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserInfoUserDetailsService userDetailsService;
    @Autowired private AuthenticationManager authenticationManager;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public String registerNewUserAccount(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email already registered";
        }
        // FRONTEND FIX: a user saved with roles = null breaks login (NPE on roles.split()).
        // Only the two known roles are accepted; anything else becomes ROLE_USER.
        if (!"ROLE_ADMIN".equals(user.getRoles())) {
            user.setRoles("ROLE_USER");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Registration successful";
    }

    // CHANGE 4: Login now authenticates via Spring's AuthenticationManager
    // and returns real JWT tokens, not a plain "Logged in" string.
    public JwtAuthResponse login(LoginRequest loginRequest) {
        // This will throw BadCredentialsException if wrong password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        logger.info("User {} logged in successfully", loginRequest.getEmail());
        return new JwtAuthResponse(accessToken, refreshToken);
    }

    // CHANGE 4: Refresh token endpoint — issue new accessToken without re-login
    public JwtAuthResponse refreshToken(String refreshToken) {
        String email = jwtUtils.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtUtils.isTokenValid(refreshToken, userDetails)) {
            throw new UserNameNotFoundException("Invalid or expired refresh token");
        }
        String newAccessToken = jwtUtils.generateAccessToken(userDetails);
        return new JwtAuthResponse(newAccessToken, refreshToken);
    }

    public String delete(User user) {
        Optional<User> existUser = userRepository.findByEmail(user.getEmail());
        if (existUser.isEmpty()) {
            return user.getEmail() + " not found.";
        }
        boolean isPasswordMatch = encoder.matches(user.getPassword(), existUser.get().getPassword());
        if (!isPasswordMatch) {
            return "Password incorrect";
        }
        userRepository.deleteById(existUser.get().getId());
        return user.getEmail() + " account deleted successfully";
    }

    // FRONTEND: used by GET /api/users/me
    public Optional<com.ecommerce.demo.dto.CurrentUserDTO> findCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new com.ecommerce.demo.dto.CurrentUserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRoles()));
    }

    public Optional<UserDTO> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserDTO(u.getUsername(), u.getEmail()));
    }

    public String loginDevice(String email, String deviceId, String location, String deviceName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNameNotFoundException("User not found: " + email));
        List<LoginDevice> devices = user.getDevices();

        for (LoginDevice device : devices) {
            if (device.getDeviceId().equals(deviceId) && device.isLoggedIn()) {
                return "Device already logged in.";
            }
        }
        for (LoginDevice device : devices) {
            if (device.getDeviceId().equals(deviceId) && !device.isLoggedIn()) {
                device.setLoggedIn(true);
                deviceRepository.save(device);
                return "Logged in back on this device.";
            }
        }
        long loggedInCount = devices.stream().filter(LoginDevice::isLoggedIn).count();
        if (loggedInCount >= 2) {
            return "Already 2 devices logged in, remove one device to login.";
        }
        LoginDevice newDevice = new LoginDevice(deviceId, location, true, deviceName, user);
        deviceRepository.save(newDevice);
        return "Device logged in successfully.";
    }

    public String logoutDevice(String email, String deviceId) {
        LoginDevice device = deviceRepository.findByDeviceIdAndEmailId(deviceId, email)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found"));
        device.setLoggedIn(false);
        deviceRepository.save(device);
        return "Device logged out successfully.";
    }

    public List<LoginDevice> getUserDevices(String email) {
        return deviceRepository.findByUserEmail(email);
    }

    public String removeDevice(Long userId, String deviceId) {
        Optional<LoginDevice> device = deviceRepository.findByDeviceIdAndUserId(deviceId, userId);
        if (device.isEmpty()) {
            return "Device not found";
        }
        deviceRepository.delete(device.get());
        return "Device removed successfully.";
    }
}
