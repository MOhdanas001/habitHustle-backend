package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.EmailReq;
import com.habithustle.habithustle_backend.DTO.LoginReq;
import com.habithustle.habithustle_backend.DTO.ResetPasswordreq;
import com.habithustle.habithustle_backend.DTO.UserRegistrationReq;
import com.habithustle.habithustle_backend.model.PasswordResetToken;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.PasswordResetRepository;
import com.habithustle.habithustle_backend.repository.UserRepository;
import com.habithustle.habithustle_backend.services.ImagekitService;
import com.habithustle.habithustle_backend.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetRepository tokenRepo;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private ImagekitService imagekitService;

    @PostMapping(
            value = "/register",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE }
    )
    public ResponseEntity<?> registerUser(
            @RequestBody(required = false) UserRegistrationReq bodyDto,
            @RequestPart(required = false) MultipartFile imageFile,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            UserRegistrationReq userDto = bodyDto;

            if (userDto == null) {
                return ResponseEntity.badRequest().body(
                        new HashMap<>() {{
                            put("status", 0);
                            put("message", "Invalid request data");
                        }}
                );
            }

            // Validation
            if (userDto.getEmail() == null || userDto.getUsername() == null) {
                return ResponseEntity.badRequest().body(
                        new HashMap<>() {{
                            put("status", 0);
                            put("message", "Missing required fields");
                        }}
                );
            }

            if (userRepository.existsByEmail(userDto.getEmail())) {
                return ResponseEntity.ok(
                        Map.of("status", 0, "message", "Email already exists")
                );
            }

            if (userRepository.existsByUsername(userDto.getUsername())) {
                return ResponseEntity.ok(
                        Map.of("status", 0, "message", "Username already exists")
                );
            }

            // Image upload
            String imageUrl = "https://cdn.default/avatar.png";
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = imagekitService.uploadProfile(imageFile);
            }

            // Create user
            User user = new User();
            user.setName(userDto.getName());
            user.setEmail(userDto.getEmail());
            user.setUsername(userDto.getUsername());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setRole("User");
            user.setProfileURL(imageUrl);

            User savedUser = userRepository.save(user);

            // Generate token and set cookie
            String token = jwtUtil.generateToken(savedUser);

            // Create cookie
            ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(60 * 60 * 24 * 7)
                    .domain("localhost") // Explicit domain
                    .build();

            System.out.println("🍪 Setting cookie: " + cookie.toString());

            // Build response
            Map<String, Object> res = new HashMap<>();
            res.put("status", 1);
            res.put("message", "User registered successfully");
            res.put("token", token);
            res.put("user", Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "name", savedUser.getName(),
                    "profileURL", savedUser.getProfileURL()
            ));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(res);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("status", 0);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> userLogin(
            @RequestBody LoginReq request,
            HttpServletResponse response
    ) {
        try {
            Optional<User> user;

            if (request.getIdentifier().contains("@")) {
                user = userRepository.findUserByEmail(request.getIdentifier());
            } else {
                user = userRepository.findByUsername(request.getIdentifier());
            }

            if (user.isEmpty() ||
                    !passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of(
                                "success", false,
                                "message", "Invalid credentials"
                        )
                );
            }

            String token = jwtUtil.generateToken(user.get());

            ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(60 * 60 * 24 * 7)
                    .domain("localhost") // Explicit domain
                    .build();


            User u = user.get();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of(
                            "success", true,
                            "message", "Login successful",
                            "token", token,
                            "user", Map.of(
                                    "id", u.getId(),
                                    "username", u.getUsername(),
                                    "email", u.getEmail(),
                                    "name", u.getName(),
                                    "profileURL", u.getProfileURL(),
                                    "role", u.getRole()
                            )
                    ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "success", false,
                            "message", "Login failed: " + e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody EmailReq email) {
        try {
            if (!userRepository.existsByEmail(email.getEmail())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", 0,
                        "message", "Email does not exist"
                ));
            }

            String newToken = generateOTP();
            tokenRepo.deleteByEmail(email.getEmail());

            PasswordResetToken token = PasswordResetToken.builder()
                    .email(email.getEmail())
                    .token(newToken)
                    .expireAt(LocalDateTime.now().plusMinutes(30))
                    .build();

            tokenRepo.save(token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email.getEmail());
            message.setSubject("Password Reset Request");
            message.setText("OTP to reset your password is: " + newToken);
            mailSender.send(message);

            return ResponseEntity.ok(Map.of(
                    "status", 1,
                    "message", "OTP sent successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 0,
                    "message", "Failed to send OTP. Please try again later."
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordreq req) {
        try {
            Optional<User> userOpt = userRepository.findUserByEmail(req.getEmail());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", 0,
                        "message", "Invalid email"
                ));
            }

            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            userRepository.save(user);

            tokenRepo.deleteByEmail(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "status", 1,
                    "message", "Password reset successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 0,
                    "message", "Something went wrong during password reset"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        System.out.println("📍 /me endpoint hit");

        // Log all cookies received
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            System.out.println("🍪 Cookies received:");
            for (Cookie c : cookies) {
                System.out.println("  - " + c.getName() + " = " + c.getValue());
            }
        } else {
            System.out.println("❌ No cookies received!");
        }

        String token = extractTokenFromCookie(request);

        if (token == null) {
            System.out.println("🔍 No token in cookie, checking Authorization header...");
            token = extractTokenFromHeader(request);
        }

        if (token == null) {
            System.out.println("❌ No token found anywhere");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "No authentication token found")
            );
        }

        System.out.println("🔑 Token found: " + token.substring(0, Math.min(20, token.length())) + "...");

        if (!jwtUtil.validateToken(token)) {
            System.out.println("❌ Token validation failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Invalid or expired token")
            );
        }

        String username = jwtUtil.extractUsername(token);
        System.out.println("👤 Username from token: " + username);

        Optional<User> userOpt = userRepository.findUserByEmail(username);

        if (userOpt.isEmpty()) {
            System.out.println("❌ User not found: " + username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "User not found")
            );
        }

        User user = userOpt.get();
        System.out.println("✅ User authenticated: " + user.getUsername());

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "profileURL", user.getProfileURL(),
                        "role", user.getRole()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .domain("localhost")
                .build();

        System.out.println("🚪 Logout - Clearing cookie");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "success", true,
                        "message", "Logged out successfully"
                ));
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("auth_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String generateOTP() {
        int otp = 10000 + new Random().nextInt(90000);
        return String.valueOf(otp);
    }
}