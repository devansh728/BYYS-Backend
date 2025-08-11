package com.byys.backend_otp.referral;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReferralEventRepository extends JpaRepository<ReferralEvent, Long> {

    Page<ReferralEvent> findByReferrerUserIdOrderByOccurredAtDesc(Long referrerUserId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r.referredUserId) FROM ReferralEvent r " +
            "WHERE r.referrerUserId = :userId AND r.eventType = 'VERIFICATION'")
    long countVerifiedReferrals(@Param("userId") Long userId);

    boolean existsByReferredUserIdAndEventType(Long referredUserId, ReferralEventType eventType);

    Optional<ReferralEvent> findByReferredUserIdAndEventType(Long referredUserId, ReferralEventType eventType);

    @Query("SELECT r.referrerUserId as referrerUserId, COUNT(r) as totalReferrals " +
            "FROM ReferralEvent r " +
            "WHERE r.eventType = 'VERIFICATION' " +
            "AND r.occurredAt BETWEEN :start AND :end " +
            "GROUP BY r.referrerUserId " +
            "ORDER BY totalReferrals DESC")
    List<LeaderboardEntry> getLeaderboard(@Param("start") Instant start,
                                          @Param("end") Instant end,
                                          Pageable pageable);

    @Query("SELECT r.referrerUserId as referrerUserId, " +
            "COUNT(r) as totalReferrals, " +
            "COUNT(DISTINCT CASE WHEN r.eventType = 'SIGNUP' THEN r.referredUserId END) as signups, " +
            "COUNT(DISTINCT CASE WHEN r.eventType = 'VERIFICATION' THEN r.referredUserId END) as verifications " +
            "FROM ReferralEvent r " +
            "WHERE r.occurredAt BETWEEN :start AND :end " +
            "GROUP BY r.referrerUserId " +
            "ORDER BY verifications DESC")
    List<LeaderboardProjection> getLeaderboardStats(@Param("start") Instant start,
                                                    @Param("end") Instant end,
                                                    Pageable pageable);

}

interface LeaderboardEntry {
    Long getReferrerUserId();
    Long getTotalReferrals();
}

interface LeaderboardProjection {
    Long getReferrerUserId();
    Long getTotalReferrals();
    Long getSignups();
    Long getVerifications();
}