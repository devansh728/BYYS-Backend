package com.byys.backend_otp.referral;

import com.byys.backend_otp.user.User;
import com.byys.backend_otp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/referrals")
@Validated
public class ReferralController {

    private final UserRepository userRepository;
    private final ReferralEventRepository referralEventRepository;
    private final ReferralTrackingService referralTrackingService;
    private final RateLimitService rateLimitService;

    public ReferralController(UserRepository userRepository,
                              ReferralEventRepository referralEventRepository,
                              ReferralTrackingService referralTrackingService,
                              RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.referralEventRepository = referralEventRepository;
        this.referralTrackingService = referralTrackingService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/share-link")
    public ResponseEntity<?> getShareLink(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "https://app.example.com/register") String baseUrl,
            @RequestParam(required = false) String source) {

        User user = userRepository.findByPhone(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String shareLink = referralTrackingService.generateShareLink(user, baseUrl, source);

        return ResponseEntity.ok(Map.of(
                "shareLink", shareLink,
                "referralCode", user.getReferralCode()
        ));
    }

    @PostMapping("/track/share")
    public ResponseEntity<?> trackShare(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String source,
            HttpServletRequest request) {

        User user = userRepository.findByPhone(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        referralTrackingService.trackShareEvent(user, source, request);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/track/click")
    public ResponseEntity<?> trackClick(
            @RequestParam String code,
            @RequestParam(required = false) String source,
            HttpServletRequest request) {

        // 10 clicks per hour per IP
        rateLimitService.checkRateLimit(
                "click:" + request.getRemoteAddr(),
                10,
                Duration.ofHours(1)
        );

        referralTrackingService.trackClickEvent(code, source, request);

        // Redirect to registration page with referral code
        return ResponseEntity.status(302)
                .header("Location", "/register?ref=" + code)
                .build();
    }

    @GetMapping("/history")
    public Page<ReferralEvent> getReferralHistory(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = userRepository.findByPhone(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        return referralEventRepository.findByReferrerUserIdOrderByOccurredAtDesc(user.getId(), pageable);
    }

    @GetMapping("/leaderboard/daily")
    public ResponseEntity<?> getDailyLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant start = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(page, size);
        List<LeaderboardProjection> stats = referralEventRepository
                .getLeaderboardStats(start, end, pageable);

        return ResponseEntity.ok(enrichWithUserDetails(stats));
    }

    @GetMapping("/leaderboard/weekly")
    public ResponseEntity<?> getWeeklyLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant start = monday.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(page, size);
        List<LeaderboardProjection> stats = referralEventRepository
                .getLeaderboardStats(start, end, pageable);

        return ResponseEntity.ok(enrichWithUserDetails(stats));
    }

    private List<Map<String, Object>> enrichWithUserDetails(List<LeaderboardProjection> stats) {
        return stats.stream().map(stat -> {
            User user = userRepository.findById(stat.getReferrerUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return Map.of(
                    "rank", stats.indexOf(stat) + 1,
                    "userId", user.getId(),
                    "name", user.getFullName(),
                    "avatar", user.getAvatarUrl(),
                    "referralCode", user.getReferralCode(),
                    "verifiedReferrals", stat.getVerifications(),
                    "totalSignups", stat.getSignups(),
                    "conversionRate", calculateConversionRate(stat.getSignups(), stat.getVerifications())
            );
        }).collect(Collectors.toList());
    }

    private double calculateConversionRate(long signups, long verifications) {
        return signups > 0 ? (verifications * 100.0 / signups) : 0;
    }
}