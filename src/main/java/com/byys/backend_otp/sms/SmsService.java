package com.byys.backend_otp.sms;

public interface SmsService {
    void sendOtp(String phoneE164, String message);
}


