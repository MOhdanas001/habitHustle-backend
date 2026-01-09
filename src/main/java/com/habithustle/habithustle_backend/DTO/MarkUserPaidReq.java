package com.habithustle.habithustle_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class MarkUserPaidReq {
    @NotBlank(message = "bet Id is required")
    private String betId;
    @NotBlank(message = "user Id is required")
    private String userId;
}
