package com.ecommerce.demo.controller;

// CHANGE 4: /login now returns JwtAuthResponse with tokens
// CHANGE 4: /refresh endpoint added for token renewal
// CHANGE 10: @Valid added to trigger request body validation

import com.ecommerce.demo.dto.CurrentUserDTO;
import com.ecommerce.demo.dto.JwtAuthResponse;
import com.ecommerce.demo.dto.LoginRequest;
import com.ecommerce.demo.dto.UserDTO;
import com.ecommerce.demo.model.LoginDevice;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody User user) {
        String result = userService.registerNewUserAccount(user);
        // FRONTEND: duplicate email now returns 409 instead of 201, so the UI can show it as an error.
        if ("Email already registered".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // FRONTEND: who am I? Returns id / username / email / roles of the token's owner.
    @GetMapping("/me")
    public ResponseEntity<CurrentUserDTO> me(java.security.Principal principal) {
        return userService.findCurrentUser(principal.getName())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.ecommerce.demo.exception.UserNameNotFoundException("User not found"));
    }

    @GetMapping("/{email}")
    public ResponseEntity<Optional<UserDTO>> getUserByEmail(@PathVariable String email) {
        Optional<UserDTO> user = userService.findUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // CHANGE 4: Returns JWT tokens on successful login
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    // CHANGE 4: New endpoint — use refreshToken to get a new accessToken
    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(userService.refreshToken(refreshToken));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUserAccount(@RequestBody User user) {
        return ResponseEntity.ok(userService.delete(user));
    }

    @PostMapping("/devices/login")
    public ResponseEntity<String> loginDevice(@RequestParam String email,
                                               @RequestParam String deviceId,
                                               @RequestParam String location,
                                               @RequestParam String deviceName) {
        return ResponseEntity.ok(userService.loginDevice(email, deviceId, location, deviceName));
    }

    @PostMapping("/devices/logout")
    public ResponseEntity<String> logoutDevice(@RequestParam String email, @RequestParam String deviceId) {
        return ResponseEntity.ok(userService.logoutDevice(email, deviceId));
    }

    @GetMapping("/devices/user/{email}")
    public ResponseEntity<List<LoginDevice>> getUserDevices(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserDevices(email));
    }

    @DeleteMapping("/devices/remove")
    public ResponseEntity<String> removeDevice(@RequestParam Long userId, @RequestParam String deviceId) {
        return ResponseEntity.ok(userService.removeDevice(userId, deviceId));
    }
}
