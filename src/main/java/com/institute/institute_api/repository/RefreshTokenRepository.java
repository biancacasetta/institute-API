package com.institute.institute_api.repository;

import com.institute.institute_api.entity.RefreshToken;
import com.institute.institute_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshToken t
        SET t.revoked = true
        WHERE t.user = :user
        AND t.revoked = false
    """)
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("""
        DELETE FROM RefreshToken t
        WHERE t.expiration < :now
    """)
    int deleteExpired(@Param("now") Instant now);
}
