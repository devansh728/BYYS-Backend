package com.byys.backend_otp.referral;

public enum ReferralEventType {
    SHARE,          // When user shares their referral link
    LINK_CLICK,     // When someone clicks the referral link
    SIGNUP,         // When referred user signs up
    VERIFICATION,   // When referred user verifies phone/email
    CONVERSION,     // When referred user completes desired action
    REWARD_CLAIMED  // When referrer claims reward
}