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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/auth")
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
            @RequestPart(required = false) UserRegistrationReq partDto,
            @RequestPart(required = false) MultipartFile imageFile,
            HttpServletRequest request) {

        try {
            // 🔥 Pick the correct DTO
            UserRegistrationReq userDto =
                    bodyDto != null ? bodyDto : partDto;

            if (userDto == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 0,
                        "message", "Invalid request data"
                ));
            }

            // 1. Validate unique fields
            if (userRepository.existsByEmail(userDto.getEmail())) {
                return ResponseEntity.ok(Map.of("status", 0, "message", "Email already exists"));
            }

            if (userRepository.existsByUsername(userDto.getUsername())) {
                return ResponseEntity.ok(Map.of("status", 0, "message", "Username already exists"));
            }

            // 2. Handle optional image
            String imageUrl = "https://cdn.default/avatar.png";
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = imagekitService.uploadProfile(imageFile);
            }

            // 3. Create User
            User user = new User();
            user.setName(userDto.getName());
            user.setEmail(userDto.getEmail());
            user.setUsername(userDto.getUsername());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setRole("User");
            user.setProfileURL(imageUrl);

            // 4. Save
            User savedUser = userRepository.save(user);

            // 5. JWT
            String token = jwtUtil.generateToken(savedUser);

            return ResponseEntity.ok(Map.of(
                    "status", 1,
                    "message", "User registered successfully",
                    "token", token,
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 0,
                    "message", "An error occurred during registration"
            ));
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

            Cookie cookie = new Cookie("auth_token", token);
            cookie.setHttpOnly(true);          // JS can't read it
            cookie.setSecure(true);            // HTTPS only (set false for local dev)
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 7);    // 1 day

            response.addCookie(cookie);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "user", Map.of(
                                    "id", user.get().getId(),
                                    "username", user.get().getUsername(),
                                    "email", user.get().getEmail()
                            )
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "success", false,
                            "message", "Login failed"
                    )
            );
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody EmailReq email) {
        try {
            System.out.println("email: " + email.getEmail());

            if (!userRepository.existsByEmail(email.getEmail())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", 0,
                        "message", "Email does not exist"
                ));
            }

            // Generate OTP
            String newToken = generateOTP();

            // Remove old OTPs
            tokenRepo.deleteByEmail(email.getEmail());

            // Save new token
            PasswordResetToken token = PasswordResetToken.builder()
                    .email(email.getEmail())
                    .token(newToken)
                    .expireAt(LocalDateTime.now().plusMinutes(30))
                    .build();

            tokenRepo.save(token);

            // Send email
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

            // Remove OTP token after successful reset
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





    public String generateOTP() {
        int otp = 10000 + new Random().nextInt(90000); // generates between 10000–99999
        return String.valueOf(otp);
    }


    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = null;
        for (Cookie c : cookies) {
            if ("auth_token".equals(c.getName())) {
                token = c.getValue();
                break;
            }
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow();

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )
        );
    }



}
