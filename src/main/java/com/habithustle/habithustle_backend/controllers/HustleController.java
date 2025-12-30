package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.DTO.UploadProofReq;
import com.habithustle.habithustle_backend.model.User;
import com.habithustle.habithustle_backend.repository.UserRepository;
import com.habithustle.habithustle_backend.services.HustleService;
import jakarta.validation.Valid;
import org.apache.http.io.SessionOutputBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public Object markUserPaid(String betId,String userId){
        return hustleService.markUserAsPaid(betId,userId);
    }

    @GetMapping("/getUsersBet")
    public Object getUsersBet(@AuthenticationPrincipal UserDetails user){
        Optional<User> authuser =userRepository.findUserByEmail(user.getUsername());
        User user1= authuser.get();
        System.out.println("getttinuer baet" +user1.getId());
        return hustleService.getUserBets(user1.getId());
    }

    @GetMapping("/viewBet")
    public Object viewBet(@RequestParam String betId){
        return hustleService.viewBet(betId);
    }

//   @PostMapping("/uploadProof")
    @PostMapping(value = "/uploadProof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object uploadProof(@ModelAttribute UploadProofReq req){
        return hustleService.uploadProofFlexible(req.getBetId(),req.getUserId(),req.getProofUrl(), req.getImageFile());
    }

//    @GetMapping("/activate-bet")
//    public Object activate







}
