package com.habithustle.habithustle_backend.services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class FirebaseNotificationService {

    private DatabaseReference dbRef;

    @Autowired
    private UserRepository userRepo;

    @PostConstruct
    public void init() {
        this.dbRef = FirebaseDatabase.getInstance().getReference();
    }

    public void sendFriendRequest(String senderId, String receiverId) {
        Map<String, Object> payload = new HashMap<>();

        Optional<User> user=  userRepo.findUserById(senderId);


        User sender=user.get();

        payload.put("type", "FRIEND_REQUEST");
        payload.put("senderId", senderId);
        payload.put("senderName", sender.getName());
        payload.put("profilepic",sender.getProfileURL());
        payload.put("username",sender.getUsername());
        payload.put("status", "PENDING");
        payload.put("timestamp", System.currentTimeMillis());
        dbRef.child("notifications")
                .child(receiverId)
                .push()
                .setValueAsync(payload);
    }

    public void sendGenericNotification(String userId, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "GENERIC");
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        dbRef.child("notifications")
                .child(userId)
                .push()
                .setValueAsync(payload);
    }
}
