package com.habithustle.habithustle_backend.controllers;

import com.habithustle.habithustle_backend.DTO.UploadProofReq;
import com.habithustle.habithustle_backend.services.HustleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proof")
public class ProofController {
    @Autowired
    private HustleService hustleService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object uploadProof(@ModelAttribute UploadProofReq req){
        return hustleService.uploadProofFlexible(req.getBetId(),req.getUserId(),req.getProofUrl(), req.getImageFile());
    }
}
