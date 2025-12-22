package com.habithustle.habithustle_backend.model;

import com.habithustle.habithustle_backend.model.bet.RequestStatus;
import com.habithustle.habithustle_backend.model.wallet.Wallet;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    @Indexed(unique = true)
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String profileURL;

    private String role;

    // Wallet and Bet are separate documents in MongoDB. Use DBRef or embedded models.
    private Wallet wallet;


    private Set<String> bets = new HashSet<>();


    private Map<String, RequestStatus> sentRequests = new HashMap<>();

    private Map<String, RequestStatus> receivedRequests = new HashMap<>();


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


}
