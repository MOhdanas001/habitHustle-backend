package com.habithustle.habithustle_backend.services;

import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.model.Hustle;
import com.habithustle.habithustle_backend.model.Proofs;
import com.habithustle.habithustle_backend.model.bet.*;
import com.habithustle.habithustle_backend.repository.HustleRepository;
import com.habithustle.habithustle_backend.repository.ProofRepository;
import com.habithustle.habithustle_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HustleService {

    @Autowired
    private HustleRepository hustleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImagekitService imagekitService;
    @Autowired
    private ProofRepository proofRepo;

    /* ---------------------------------------------------
       CREATE BET
    --------------------------------------------------- */
    public Object createBet(SearchRequest.BetRequestDTO req) {
        try {

            if (req.getStartDate().isBefore(LocalDateTime.now())) {
                return Map.of(
                        "status", 0,
                        "message", "Start Date must be in future"
                );
            }

            List<SearchRequest.Participants> participants =
                    req.getParticipantIds().stream()
                            .map(id -> SearchRequest.Participants.builder()
                                    .userId(id)
                                    .paymentStatus(PaymentStatus.UNPAID)
                                    .betStatus(BetParticipationStatus.NOT_STARTED)
                                    .proofs(new HashMap<>())
                                    .usedOffDays(0)
                                    .build())
                            .toList();

            Hustle bet = Hustle.builder()
                    .name(req.getName())
                    .description(req.getDescription())
                    .amount(req.getAmount())
                    .participants(participants)
                    .verifierId(req.getVerifierId())
                    .taskDays(req.getTaskDays())
                    .allowedOffDays(req.getAllowedOffDays())
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .betStatus(BetStatus.NOT_STARTED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Hustle saved = hustleRepository.save(bet);

            req.getParticipantIds().forEach(uid ->
                    userRepository.findById(uid).ifPresent(u -> {
                        u.getBets().add(saved.getId());
                        u.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(u);
                    })
            );

            return Map.of(
                    "status", 1,
                    "message", "Bet created",
                    "betId", saved.getId()
            );

        } catch (Exception e) {
            // Optional: log the exception
            // log.error("Error creating bet", e);

            return Map.of(
                    "status", 0,
                    "message", "Failed to create bet",
                    "error", e.getMessage()
            );
        }
    }

    /* ---------------------------------------------------
       MARK PAYMENT
    --------------------------------------------------- */
    public Object markUserAsPaid(String betId, String userId) {

        Hustle bet = hustleRepository.findById(betId)
                .orElseThrow(() -> {
                    return new RuntimeException("Bet not found");
                });

        SearchRequest.Participants participant = bet.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not in bet"));

        if (participant.getPaymentStatus() == PaymentStatus.PAID) {
            return Map.of("status", 0, "message", "Already paid");
        }

        participant.setPaymentStatus(PaymentStatus.PAID);

        boolean allPaid = bet.getParticipants().stream()
                .allMatch(p -> p.getPaymentStatus() == PaymentStatus.PAID);

        if (allPaid) {
            bet.setBetStatus(BetStatus.ACTIVE);
            bet.getParticipants()
                    .forEach(p -> p.setBetStatus(BetParticipationStatus.ACTIVE));
        }

        bet.setUpdatedAt(LocalDateTime.now());
        hustleRepository.save(bet);

        return Map.of("status", 1, "message", "Payment recorded");
    }

    /* ---------------------------------------------------
       VIEW BET
    --------------------------------------------------- */
    public Object viewBet(String betId) {
        Hustle bet = hustleRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet not found"));

        return Map.of("status", 1, "data", bet);
    }

    /* ---------------------------------------------------
       USER BETS
    --------------------------------------------------- */
    public Object getUserBets(String userId) {

        List<Hustle> bets = hustleRepository.findByParticipantsUserId(userId);

        List<Map<String, Object>> upcoming = new ArrayList<>();
        List<Map<String, Object>> active = new ArrayList<>();
        List<Map<String, Object>> completed = new ArrayList<>();

        for (Hustle bet : bets) {

            SearchRequest.Participants participant =
                    bet.getParticipants().stream()
                            .filter(p -> p.getUserId().equals(userId))
                            .findFirst()
                            .orElse(null);

            Integer participantsCount=bet.getParticipants().size();

            Map<String, Object> betData = Map.of(
                    "betId", bet.getId(),
                    "name", bet.getName(),
                    "description", bet.getDescription(),
                    "betStatus", bet.getBetStatus(),
                    "total_amount",bet.getAmount(),
                    "amount",bet.getAmount()/participantsCount,
                    "start_date",bet.getStartDate(),
                    "end_date",bet.getEndDate(),
                    "participantStatus", participant
            );

            switch (bet.getBetStatus()) {
                case NOT_STARTED -> {
                    upcoming.add(betData);
                }
                case ACTIVE -> {
                    active.add(betData);
                }
                case COMPLETED -> {
                    completed.add(betData);
                }
            }
        }


        return Map.of(
                "status", 1,
                "data", Map.of(
                        "upcoming", upcoming,
                        "active", active,
                        "completed", completed
                )
        );
    }

    /* ---------------------------------------------------
       UPLOAD PROOF
    --------------------------------------------------- */
    public Object uploadProofFlexible(
            String betId,
            String userId,
            String proofUrl,
            MultipartFile imageFile) {

        Hustle bet = hustleRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet not found"));

        SearchRequest.Participants participant = bet.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not in bet"));

        String finalUrl = proofUrl;
        String type = "url";

        if (imageFile != null && !imageFile.isEmpty()) {
            finalUrl = imagekitService.uploadProof(imageFile);
            type = "image";
        }

        if (finalUrl == null || finalUrl.isBlank()) {
            return Map.of("status", 0, "message", "No proof provided");
        }

        Proofs proof = proofRepo.save(
                Proofs.builder()
                        .betId(betId)
                        .participantId(userId)
                        .verifierId(bet.getVerifierId())
                        .proof(finalUrl)
                        .proofType(type)
                        .status(ProofStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        participant.getProofs().put(proof.getId(), ProofStatus.PENDING);
        participant.setBetStatus(BetParticipationStatus.ACTIVE);

        bet.setUpdatedAt(LocalDateTime.now());
        hustleRepository.save(bet);

        return Map.of("status", 1, "message", "Proof uploaded");
    }

    /* ---------------------------------------------------
       VERIFY PROOF (SECURE)
    --------------------------------------------------- */
    public Object verifyProof(
            String betId,
            String proofId,
            String verifierId,
            boolean verified) {

        Hustle bet = hustleRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet not found"));

        if (!bet.getVerifierId().equals(verifierId)) {
            return Map.of("status", 0, "message", "Unauthorized");
        }

        Proofs proof = proofRepo.findById(proofId)
                .orElseThrow(() -> new RuntimeException("Proof not found"));

        SearchRequest.Participants participant = bet.getParticipants().stream()
                .filter(p -> p.getUserId().equals(proof.getParticipantId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        ProofStatus status = verified ? ProofStatus.VERIFIED : ProofStatus.REJECTED;

        participant.getProofs().put(proofId, status);
        proof.setStatus(status);
        proof.setUpdatedAt(LocalDateTime.now());

        if (!verified) {
            participant.setBetStatus(BetParticipationStatus.FAILED);
        }

        bet.setUpdatedAt(LocalDateTime.now());

        proofRepo.save(proof);
        hustleRepository.save(bet);

        return Map.of("status", 1, "message",
                verified ? "Proof verified" : "Proof rejected");
    }
}
