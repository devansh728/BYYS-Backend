package com.byys.backend_otp.auth;

import com.byys.backend_otp.otp.OtpRateLimitException;
import com.byys.backend_otp.otp.OtpService;
import com.byys.backend_otp.referral.ReferralTrackingService;
import com.byys.backend_otp.security.JwtService;
import com.byys.backend_otp.sms.SmsService;
import com.byys.backend_otp.user.User;
import com.byys.backend_otp.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth/otp")
@Validated
public class AuthController {

    private final OtpService otpService;
    private final SmsService smsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ReferralTrackingService referralTrackingService;

    public AuthController(OtpService otpService, SmsService smsService, JwtService jwtService, UserRepository userRepository, ReferralTrackingService referralTrackingService) {
        this.otpService = otpService;
        this.smsService = smsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.referralTrackingService = referralTrackingService;
    }

    public record SendOtpRequest(@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone) {}
    public record VerifyOtpRequest(@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone,
                                   @NotBlank String otp,
                                   String referralCode,
                                   String fullName) {}

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            String otp = otpService.generateAndStore(request.phone());
            String message = "Your OTP for ReferralApp is " + otp + ". Valid for 5 minutes.";
            smsService.sendOtp(request.phone(), message);
            return ResponseEntity.accepted().build();
        } catch (OtpRateLimitException e) {
            return ResponseEntity.status(429).body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean ok = otpService.verifyAndInvalidate(request.phone(), request.otp());
        if (!ok) {
            return ResponseEntity.status(401).body("Invalid or expired OTP");
        }

        Optional<User> existing = userRepository.findByPhone(request.phone());
        User user = existing.orElseGet(() -> {
            User u = new User();
            u.setPhone(request.phone());
            u.setFullName(request.fullName);
            u.setReferralCode(generateUniqueReferralCode());
            u.setVerified(true);
            if (request.referralCode() != null && !request.referralCode().isBlank()) {
                userRepository.findByReferralCode(request.referralCode()).ifPresent(referrer -> {
                    u.setReferredByCode(request.referralCode());
                    referralTrackingService.trackSignupEvent(u.getId(), request.referralCode());
                });
            }
            u.setFullName(request.fullName());
            return userRepository.save(u);
        });

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generate(user.getPhone());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public record TokenResponse(String token) {}

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }
}


