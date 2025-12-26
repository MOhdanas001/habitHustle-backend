package com.habithustle.habithustle_backend.DTO;

import com.habithustle.habithustle_backend.model.bet.BetParticipationStatus;
import com.habithustle.habithustle_backend.model.bet.PaymentStatus;
import com.habithustle.habithustle_backend.model.bet.ProofStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SearchRequest {
    private String val;

    @Data
    public static class BetRequestDTO {
        @NotBlank(message = "Name is required")
        private String name;

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        private Double amount;

        @NotEmpty(message = "Participant IDs are required")
        private List<@NotBlank(message = "Participant ID cannot be blank") String> participantIds;

        @NotBlank(message = "Verifier ID is required")
        private String verifierId;

        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        private LocalDateTime startDate;

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        private LocalDateTime endDate;

        @NotEmpty(message = "Task days are required")
        private List<DayOfWeek> taskDays;

        @NotNull(message = "Allowed off days is required")
        @Min(value = 0, message = "Allowed off days cannot be negative")
        private Integer allowedOffDays;
        }

    @Data
    @Builder
    public static class Participants {
        private String userId;
        private PaymentStatus paymentStatus;
        private BetParticipationStatus betStatus;
        private Map<String, ProofStatus> proofs; // proofId -> status
        private Integer usedOffDays;
    }
}
