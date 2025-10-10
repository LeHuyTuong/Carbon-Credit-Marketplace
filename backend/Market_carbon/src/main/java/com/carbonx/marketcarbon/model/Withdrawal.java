package com.carbonx.marketcarbon.model;

import com.carbonx.marketcarbon.common.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Status  status;

    private Long amount;

    @ManyToOne
    private User user;

    private LocalDateTime createdAt;
}
