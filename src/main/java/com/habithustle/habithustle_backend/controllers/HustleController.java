package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.MarkUserPaidReq;
import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.DTO.UploadProofReq;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.UserRepository;
import com.habithustle.habithustle_backend.services.HustleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("api/bet")
public class HustleController
{
    @Autowired
    private HustleService hustleService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public Object createHustle(@Valid @RequestBody SearchRequest.BetRequestDTO req){
        return hustleService.createBet(req);
    }

    @PostMapping("/markUserPaid")
    public Object markUserPaid(@Valid @RequestBody MarkUserPaidReq req){
        return hustleService.markUserAsPaid(req.getBetId(), req.getUserId());
    }

    @GetMapping("/getUsersBet")
    public Object getUsersBet(@AuthenticationPrincipal UserDetails user){
        Optional<User> authuser =userRepository.findUserByEmail(user.getUsername());
        User user1= authuser.get();
        return hustleService.getUserBets(user1.getId());
    }

    @GetMapping("/viewBet")
    public Object viewBet(@RequestParam String betId,@AuthenticationPrincipal UserDetails user){

        return hustleService.viewBet(betId,user.getUsername());
    }

//   @PostMapping("/uploadProof")










}
