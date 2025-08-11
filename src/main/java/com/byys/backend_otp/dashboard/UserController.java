package com.byys.backend_otp.dashboard;

import com.byys.backend_otp.referral.ReferralEvent;
import com.byys.backend_otp.referral.ReferralEventRepository;
import com.byys.backend_otp.user.User;
import com.byys.backend_otp.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/me")
public class UserController {

    private final UserRepository userRepository;
    private final ReferralEventRepository referralEventRepository;

    public UserController(UserRepository userRepository, ReferralEventRepository referralEventRepository) {
        this.userRepository = userRepository;
        this.referralEventRepository = referralEventRepository;
    }

    @GetMapping
    public ResponseEntity<?> profile(@AuthenticationPrincipal UserDetails principal) {
        String phone = principal.getUsername();
        User user = userRepository.findByPhone(phone).orElseThrow();
        long total = referralEventRepository.count(); // simplified; can refine to only conversions

        Map<String, Object> dto = new HashMap<>();
        dto.put("phone", user.getPhone());
        dto.put("fullName", user.getFullName());
        dto.put("referralCode", user.getReferralCode());
        dto.put("totalReferrals", total);
        dto.put("recentActivity", referralEventRepository
            .findByReferrerUserIdOrderByOccurredAtDesc(user.getId(), PageRequest.of(0, 20))
            .map(ReferralEvent::getEventType));
        return ResponseEntity.ok(dto);
    }
}


