package com.habithustle.habithustle_backend.services;

import com.habithustle.habithustle_backend.DTO.ApiResponse;
import com.habithustle.habithustle_backend.DTO.FriendListRes;
import com.habithustle.habithustle_backend.DTO.FriendsList;
import com.habithustle.habithustle_backend.DTO.SearchResponse;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.model.bet.RequestStatus;
import com.habithustle.habithustle_backend.repository.FriendRequestRepository;
import com.habithustle.habithustle_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

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
    private FirebaseNotificationService firebaseService;

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
        System.out.println("request Data updated");
        userRepo.save(sender);
        userRepo.save(receiver);
        firebaseService.sendFriendRequest(senderId, receiverId);
        return new ApiResponse(true, "Friend request sent successfully");
    }


    public ApiResponse respondToRequest(
            String senderId,
            String receiverId,
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
            firebaseService.deleteFriendRequest(senderId,receiverId);
            return new ApiResponse(true, "Friend request accepted");
        }

        // Reject → remove from both users
        receiver.getReceivedRequests().remove(senderId);
        sender.getSentRequests().remove(receiverId);

        userRepo.save(receiver);
        userRepo.save(sender);

        firebaseService.deleteFriendRequest(senderId,receiverId);

        return new ApiResponse(true, "Friend request rejected");
    }



    public FriendListRes<List<FriendsList>> getFriends(String userId) {

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

        List<FriendsList> friends = userRepo.findAllById(friendIds).stream()
                .map(u -> new FriendsList (
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getName()
                        ))
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
