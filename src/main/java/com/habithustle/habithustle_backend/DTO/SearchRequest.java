package com.habithustle.habithustle_backend.DTO;

import com.habithustle.habithustle_backend.model.bet.BetParticipationStatus;
import com.habithustle.habithustle_backend.model.bet.PaymentStatus;
import com.habithustle.habithustle_backend.model.bet.ProofStatus;
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
            private String name;
            private String description;
            private Double amount;

            private List<String> participantIds;
            private String verifierId;

            private LocalDateTime startDate;
            private LocalDateTime endDate;

            private List<DayOfWeek> taskDays;
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
