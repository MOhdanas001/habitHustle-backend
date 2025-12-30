package com.habithustle.habithustle_backend.model;

import com.habithustle.habithustle_backend.DTO.FriendsList;
import com.habithustle.habithustle_backend.DTO.SearchRequest;
import com.habithustle.habithustle_backend.model.bet.BetStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "hustle")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Hustle
{
    @Id
    private String id;
    private String name;
    private String description;
    private Double amount;
    private String status;
    private String proofDescription;
    private List<SearchRequest.Participants> participants;

    private BetStatus betStatus; // NOT_STARTED, ACTIVE, COMPLETED
    private FriendsList winner;

    private Integer betProgress;


    private String verifierId;                   // Optional: can also be one of the participants

    private List<DayOfWeek> taskDays;            // Required activity days
    private Integer allowedOffDays;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
