package com.carbonx.marketcarbon.repository;


import com.carbonx.marketcarbon.common.RegistrationStatus;
import com.carbonx.marketcarbon.common.USER_STATUS;
import com.carbonx.marketcarbon.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findByEmail(String username);
    boolean existsByEmail(String email);
    void deleteByStatusAndOtpExpiryDateBefore(USER_STATUS status, LocalDateTime dateTime);

        // Lấy danh sách user theo role name
        @Query("""
           SELECT DISTINCT u
           FROM User u
           JOIN u.roles r
           WHERE r.name = :roleName
           """)
        List<User> findByRoleName(@Param("roleName") String roleName);

        // Lấy danh sách user theo role name + keyword (fullName hoặc email)
        @Query("""
           SELECT DISTINCT u
           FROM User u
           JOIN u.roles r
           WHERE r.name = :roleName
             AND (
                 LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
             )
           """)
        Page<User> searchByRoleNameAndKeyword(@Param("roleName") String roleName,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);

        // Lấy user theo role name + status
        @Query("""
           SELECT DISTINCT u
           FROM User u
           JOIN u.roles r
           WHERE r.name = :roleName
             AND u.status = :status
           """)
        Page<User> findByRoleNameAndStatus(@Param("roleName") String roleName,
                                           @Param("status") USER_STATUS status,
                                           Pageable pageable);

    }
