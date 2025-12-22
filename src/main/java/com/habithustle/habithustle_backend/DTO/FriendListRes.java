package com.habithustle.habithustle_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendListRes<T> {
        private boolean success;
        private String message;
        private T data;
}
