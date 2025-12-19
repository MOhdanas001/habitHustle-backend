package com.habithustle.habithustle_backend.model;

import com.habithustle.habithustle_backend.model.bet.ProofStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "proofs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Proofs {

    @Id
    private String id;

    private String proof;
    private String participantId;

    private ProofStatus status;

    private String betId;

    private String verifierId;

    private String proofType;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
