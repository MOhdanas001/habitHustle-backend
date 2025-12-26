package com.habithustle.habithustle_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class FriendsList {
    private String id;
    private String username;
    private String email;
    private String name;
}
