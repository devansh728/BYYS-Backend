package com.byys.backend_otp.referral;

import com.byys.backend_otp.user.User;
import com.byys.backend_otp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class ReferralTrackingService {

    private final ReferralEventRepository referralEventRepository;
    private final UserRepository userRepository;

    public ReferralTrackingService(ReferralEventRepository referralEventRepository,
                                   UserRepository userRepository) {
        this.referralEventRepository = referralEventRepository;
        this.userRepository = userRepository;
    }

    public String generateShareLink(User user, String baseUrl, String source) {
        // Track share event
        trackShareEvent(user, source, null);

        // Generate unique tracking link
        return baseUrl + "?ref=" + user.getReferralCode() +
                (source != null ? "&source=" + source : "");
    }

    @Transactional
    public void trackShareEvent(User user, String source, HttpServletRequest request) {
        ReferralEvent event = new ReferralEvent(user.getId(), ReferralEventType.SHARE);

        if (request != null) {
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setIpAddress(request.getRemoteAddr());
        }

        if (source != null) {
            event.setReferralSource(source);
        }

        referralEventRepository.save(event);
    }

    @Transactional
    public void trackClickEvent(String referralCode, String source, HttpServletRequest request) {
        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new RuntimeException("Invalid referral code"));

        ReferralEvent event = new ReferralEvent(referrer.getId(), ReferralEventType.LINK_CLICK);

        if (request != null) {
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setIpAddress(request.getRemoteAddr());

            // Store cookie for attribution if user signs up later
            String clickId = UUID.randomUUID().toString();
            // In practice, you'd set this in the response cookie
        }

        if (source != null) {
            event.setReferralSource(source);
        }

        referralEventRepository.save(event);
    }

    @Transactional
    public void trackSignupEvent(Long referredUserId, String referralCode) {
        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new RuntimeException("Invalid referral code"));

        // Prevent self-referral
        if (referrer.getId().equals(referredUserId)) {
            throw new IllegalArgumentException("Self-referrals are not allowed");
        }

        // Ensure we don't duplicate events
        if (!referralEventRepository.existsByReferredUserIdAndEventType(referredUserId, ReferralEventType.SIGNUP)) {
            ReferralEvent event = new ReferralEvent(referrer.getId(), ReferralEventType.SIGNUP);
            event.setReferredUserId(referredUserId);
            referralEventRepository.save(event);
        }
    }

    @Transactional
    public void trackVerificationEvent(Long referredUserId) {
        // First verify the user exists
        User referredUser = userRepository.findById(referredUserId)
                .orElseThrow(() -> new IllegalArgumentException("Referred user not found"));

        // Find the original signup event
        referralEventRepository.findByReferredUserIdAndEventType(referredUserId, ReferralEventType.SIGNUP)
                .ifPresentOrElse(
                        signupEvent -> {
                            // Verify the referrer exists
                            User referrer = userRepository.findById(signupEvent.getReferrerUserId())
                                    .orElseThrow(() -> new IllegalStateException("Referrer user not found"));

                            // Prevent self-referral (additional safety check)
                            if (referrer.getId().equals(referredUserId)) {
                                throw new IllegalStateException("Self-referral detected");
                            }

                            // Create verification event
                            ReferralEvent event = new ReferralEvent(
                                    referrer.getId(),
                                    ReferralEventType.VERIFICATION
                            );
                            event.setReferredUserId(referredUserId);
                            referralEventRepository.save(event);

                            // You could add additional logic here like:
                            // - Reward the referrer
                            // - Send notification
                            // - Update leaderboard cache
                        },
                        () -> log.warn("No signup event found for user {}", referredUserId)
                );
    }
}