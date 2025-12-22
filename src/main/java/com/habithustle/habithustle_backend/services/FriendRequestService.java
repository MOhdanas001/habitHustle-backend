package com.habithustle.habithustle_backend.services;

import com.habithustle.habithustle_backend.DTO.ApiResponse;
import com.habithustle.habithustle_backend.DTO.FriendListRes;
import com.habithustle.habithustle_backend.DTO.FriendRequestEvent;
import com.habithustle.habithustle_backend.DTO.SearchResponse;
import com.habithustle.habithustle_backend.model.FriendRequest;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.model.bet.RequestStatus;
import com.habithustle.habithustle_backend.repository.FriendRequestRepository;
import com.habithustle.habithustle_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FriendRequestService {

    @Autowired
    private FriendRequestRepository friendRequestRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ApiResponse sendRequest(String senderId, String receiverId) {

        if (senderId == null || receiverId == null) {
            return new ApiResponse(false, "Invalid user id");
        }

        if (senderId.equals(receiverId)) {
            return new ApiResponse(false, "You cannot send a request to yourself");
        }

        User sender = userRepo.findUserById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepo.findUserById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Already friends or request exists
        RequestStatus existingStatus = sender.getSentRequests().get(receiverId);
        if (existingStatus != null) {
            if (existingStatus == RequestStatus.ACCEPTED) {
                return new ApiResponse(false, "You are already friends");
            }
            if (existingStatus == RequestStatus.PENDING) {
                return new ApiResponse(false, "Friend request already sent");
            }
        }

        // Defensive check (receiver side)
        if (receiver.getReceivedRequests().containsKey(senderId)) {
            return new ApiResponse(false, "Request already exists");
        }

        // Update both users
        sender.getSentRequests().put(receiverId, RequestStatus.PENDING);
        receiver.getReceivedRequests().put(senderId, RequestStatus.PENDING);

        userRepo.save(sender);
        userRepo.save(receiver);

        // WebSocket events (non-blocking, failure-safe)
        try {
            FriendRequestEvent event = new FriendRequestEvent(
                    "FRIEND_REQUEST_RECEIVED",
                    senderId,
                    receiverId,
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/friend-requests",
                    event
            );

            messagingTemplate.convertAndSendToUser(
                    senderId,
                    "/queue/friend-requests",
                    new FriendRequestEvent(
                            "FRIEND_REQUEST_SENT",
                            senderId,
                            receiverId,
                            LocalDateTime.now()
                    )
            );
        } catch (Exception e) {
            System.err.println("WebSocket delivery failed: " + e.getMessage());
        }

        return new ApiResponse(true, "Friend request sent successfully");
    }


    public ApiResponse respondToRequest(
            String receiverId,
            String senderId,
            boolean accept
    ) {

        if (receiverId == null || senderId == null) {
            return new ApiResponse(false, "Invalid user id");
        }

        if (receiverId.equals(senderId)) {
            return new ApiResponse(false, "Invalid request");
        }

        User receiver = userRepo.findUserById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        User sender = userRepo.findUserById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        RequestStatus status = receiver.getReceivedRequests().get(senderId);

        if (status == null) {
            return new ApiResponse(false, "Friend request not found");
        }

        if (status != RequestStatus.PENDING) {
            return new ApiResponse(false, "Request already responded");
        }

        if (accept) {
            receiver.getReceivedRequests().put(senderId, RequestStatus.ACCEPTED);
            sender.getSentRequests().put(receiverId, RequestStatus.ACCEPTED);

            userRepo.save(receiver);
            userRepo.save(sender);

            return new ApiResponse(true, "Friend request accepted");
        }

        // Reject → remove from both users
        receiver.getReceivedRequests().remove(senderId);
        sender.getSentRequests().remove(receiverId);

        userRepo.save(receiver);
        userRepo.save(sender);

        return new ApiResponse(true, "Friend request rejected");
    }



    public FriendListRes<List<SearchResponse>> getFriends(String userId) {

        User user = userRepo.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<String> friendIds = new HashSet<>();

        // Friends from sent requests
        user.getSentRequests().forEach((id, status) -> {
            if (status == RequestStatus.ACCEPTED) {
                friendIds.add(id);
            }
        });

        // Friends from received requests
        user.getReceivedRequests().forEach((id, status) -> {
            if (status == RequestStatus.ACCEPTED) {
                friendIds.add(id);
            }
        });

        if (friendIds.isEmpty()) {
            return new FriendListRes<>(true, "No friends found", List.of());
        }

        List<SearchResponse> friends = userRepo.findAllById(friendIds).stream()
                .map(u -> new SearchResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getProfileURL()))
                .toList();

        return new FriendListRes<>(
                true,
                "Friends fetched successfully",
                friends
        );
    }

    public FriendListRes<List<SearchResponse>> getPendingRequests(String receiverId) {

        User receiver = userRepo.findUserById(receiverId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SearchResponse> pendingRequests = receiver.getReceivedRequests()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() == RequestStatus.PENDING)
                .map(entry -> userRepo.findUserById(entry.getKey())
                        .map(user -> new SearchResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getProfileURL()
                        ))
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .toList();

        return new FriendListRes<>(
                true,
                "Pending friend requests fetched",
                pendingRequests
        );
    }

}
