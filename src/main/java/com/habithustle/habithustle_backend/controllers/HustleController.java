package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.DTO.UploadProofReq;
import com.habithustle.habithustle_backend.services.HustleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/bet")
public class HustleController
{
    @Autowired
    private HustleService hustleService;

    @PostMapping("/create")
    public Object createHustle(@Valid @RequestBody SearchRequest.BetRequestDTO req){
        return hustleService.createBet(req);
    }

    @PostMapping("/markUserPaid")
    public Object markUserPaid(String betId,String userId){
        return hustleService.markUserAsPaid(betId,userId);
    }

    @GetMapping("/getUsersBet")
    public Object getUsersBet(@RequestParam String userId){
        return hustleService.getUserBets(userId);
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
