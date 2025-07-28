package com.habithustle.habithustle_backend.services;

import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.model.Hustle;
import com.habithustle.habithustle_backend.model.Proofs;
import com.habithustle.habithustle_backend.model.bet.*;
import com.habithustle.habithustle_backend.repository.HustleRepository;
import com.habithustle.habithustle_backend.repository.ProofRepository;
import com.habithustle.habithustle_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private UserRepository  userRepository;
    @Autowired
    private ImagekitService imagekitService;
    @Autowired
    private ProofRepository proofRepo;

    public Object createBet(SearchRequest.BetRequestDTO req){

        if(req.getStartDate().isBefore(LocalDateTime.now())){
            return Map.of("status",0,
                   "message","Start Date must be in Future"
                     );
        }

        List<SearchRequest.Participants> participants = req.getParticipantIds().stream().map(id ->
                SearchRequest.Participants.builder()
                        .userId(id)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .betStatus(BetParticipationStatus.NOT_STARTED)
                        .proofs(new HashMap<>())
                        .usedOffDays(0)
                        .build()
        ).toList();

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
                .generalProofs(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Hustle saved = hustleRepository.save(bet);

        //Save betId into users
        for (String userId : req.getParticipantIds()) {
            userRepository.findById(userId).ifPresent(user -> {
                user.getBets().add(saved.getId());
                user.setUpdatedAt(LocalDateTime.now()); // if you track updatedAt
                userRepository.save(user);
            });
        }



        //Notify users via email or in-app
        return Map.of("status", 1, "message", "Bet created successfully", "betId", saved.getId());

    }


    public Object markUserAsPaid(String betId, String userId) {
        Optional<Hustle> optional = hustleRepository.findById(betId);
        if (optional.isEmpty()) return Map.of("status", 0, "message", "Bet not found");

        Hustle bet = optional.get();
        boolean updated = false;

        for (SearchRequest.Participants p : bet.getParticipants()) {
            if (p.getUserId().equals(userId) && p.getPaymentStatus() == PaymentStatus.UNPAID) {
                p.setPaymentStatus(PaymentStatus.PAID);
                updated = true;
            }
        }

        if (!updated) return Map.of("status", 0, "message", "User already paid or not in bet");

        // Check if all paid
        boolean allPaid = bet.getParticipants().stream()
                .allMatch(p -> p.getPaymentStatus() == PaymentStatus.PAID);

        if (allPaid) {
            bet.setBetStatus(BetStatus.ACTIVE);
            bet.getParticipants().forEach(p -> p.setBetStatus(BetParticipationStatus.ACTIVE));
        }

        bet.setUpdatedAt(LocalDateTime.now());
        hustleRepository.save(bet);

        return Map.of("status", 1, "message", "Payment recorded");
    }

    public Object ViewBet(String betId){
        Optional<Hustle> optional = hustleRepository.findById(betId);
        if (optional.isEmpty()) return Map.of("status", 0, "message", "Bet not found");

        Hustle bet = optional.get();
       return Map.of(
         "status",1,
         "data",bet
       );

    }

    public Object getUserBets(String userId) {
        List<Hustle> allUserBets = hustleRepository.findByParticipantsUserId(userId);

        List<Map<String, Object>> response = allUserBets.stream().map(bet -> {
            LocalDate today = LocalDate.now();
            LocalDate start = bet.getStartDate().toLocalDate();
            LocalDate end = bet.getEndDate().toLocalDate();

            long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
            long elapsedDays = ChronoUnit.DAYS.between(start, today) + 1;

            if (elapsedDays < 0) elapsedDays = 0;
            if (elapsedDays > totalDays) elapsedDays = totalDays;


            Map<String, Object> betData = new HashMap<>();
            betData.put("betId", bet.getId());
            betData.put("name", bet.getName());
            betData.put("description", bet.getDescription());
            betData.put("progress", elapsedDays/totalDays);
            betData.put("participantStatus",bet.getParticipants().stream().filter(p->p.getUserId().equals(userId)).findFirst());

            return betData;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("data", response);
        result.put("status", 1);

        return result;
    }

    public Object uploadProofFlexible(String betId, String userId, String proofUrl, MultipartFile imageFile) {
        Optional<Hustle> optBet = hustleRepository.findById(betId);
        if (optBet.isEmpty()) return Map.of("status", 0, "message", "Bet not found");

        Hustle bet = optBet.get();
        LocalDate proofDate =LocalDate.now();

        Optional<SearchRequest.Participants> participantOpt = bet.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst();

        if (participantOpt.isEmpty()) return Map.of("status", 0, "message", "User not in bet");

        var participant = participantOpt.get();

        String finalProofUrl = proofUrl;
        var proofType="url";

        if (imageFile != null && !imageFile.isEmpty()) {
            // Save locally or upload to cloud and get URL
            finalProofUrl = imagekitService.uploadProof(imageFile);
            proofType="image";
        }

        if (finalProofUrl == null || finalProofUrl.isBlank()) {
            return Map.of("status", 0, "message", "No proof provided");
        }

        String combined = finalProofUrl;

        if (participant.getProofs().containsKey(combined)) {
            return Map.of("status", 0, "message", "Proof already submitted for this date");
        }



       Proofs proof=Proofs.builder()
               .proof(combined)
               .participantId(userId)
               .Status(0)
               .betId(betId)
               .verifierId(bet.getVerifierId())
               .proofType(proofType)
               .build();

       proofRepo.save(proof);

        participant.getProofs().put(proof.getId(),0);
        participant.setBetStatus(BetParticipationStatus.ACTIVE);
        bet.setUpdatedAt(LocalDateTime.now());
        hustleRepository.save(bet);

       return Map.of("status", 1, "message", "Proof uploaded", "proof", combined);
    }

//    public Object verifyProof(String proofId,String betId,Boolean verified){
//
//        Optional<Hustle> bet=hustleRepository.findById(betId);
//
//
//
//    }




}
