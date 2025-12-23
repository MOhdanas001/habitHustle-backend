package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.FriendListRes;
import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.DTO.SearchResponse;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.UserRepository;
import com.habithustle.habithustle_backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/search/users")
    public ResponseEntity<FriendListRes<List<SearchResponse>>> searchUsers(
            @RequestParam("q") String query
    ) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new FriendListRes<>(false, "Search query is required", List.of()));
        }
        List<SearchResponse> result = userRepository
                .searchByUsername(query)
                .stream()
                .map(user -> new SearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getProfileURL()
                ))
                .toList();

        return ResponseEntity.ok(
                new FriendListRes<>(true, "Users fetched", result)
        );
    }



}
