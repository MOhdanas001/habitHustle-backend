package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.ApiResponse;
import com.habithustle.habithustle_backend.DTO.RespondRequest;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.UserRepository;
import com.habithustle.habithustle_backend.services.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendRequestService service;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity <ApiResponse> sendRequest(
            @RequestParam String toUserId,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Unauthorized"));
        }

        String senderEmail = user.getUsername();

        User sender = userRepository.findUserByEmail(senderEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Sender not found"));

        ApiResponse response =
                service.sendRequest(sender.getId(), toUserId);

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


    @PostMapping("/respond")
    public ResponseEntity<?> respondToRequest(
            @RequestBody RespondRequest req,
            @AuthenticationPrincipal UserDetails user) {

        String senderEmail=user.getUsername();
        User receiver = userRepository.findUserByEmail(senderEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Sender not found"));

        String receiverId = receiver.getId();
        ApiResponse response = service.respondToRequest(req.getSenderId(), receiverId, req.getAccept());
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getPending(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getPendingRequests(user.getId()));
    }

    @GetMapping("/pending-request")
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getFriends(user.getId()));
    }
}

