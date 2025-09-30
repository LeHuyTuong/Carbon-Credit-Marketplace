package com.carbonx.marketcarbon.repository;



import com.carbonx.marketcarbon.domain.USER_STATUS;
import com.carbonx.marketcarbon.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findByEmail(String username);
    boolean existsByEmail(String email);
    void deleteByStatusAndOtpExpiredAtBefore(USER_STATUS status, OffsetDateTime time);
}
