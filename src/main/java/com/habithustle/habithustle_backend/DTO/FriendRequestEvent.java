package com.habithustle.habithustle_backend.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FriendRequestEvent {
    private String type; // FRIEND_REQUEST_SENT, FRIEND_REQUEST_RECEIVED
    private String fromUserId;
    private String toUserId;
    private LocalDateTime timestamp;
}
