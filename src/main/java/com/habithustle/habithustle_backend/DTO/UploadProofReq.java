package com.habithustle.habithustle_backend.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadProofReq {
    private String userId;
    private String betId;
    private String proofUrl;
    private MultipartFile imageFile;
}
