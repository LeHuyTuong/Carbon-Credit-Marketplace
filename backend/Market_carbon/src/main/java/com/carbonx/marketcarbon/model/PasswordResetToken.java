package com.carbonx.marketcarbon.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    private @NonNull String token;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    private @NonNull User user;

    private @NonNull Date expiryDate;

    public boolean isExpired() {
        return expiryDate.before(new Date());
    }
}
