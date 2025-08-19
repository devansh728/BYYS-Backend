package com.byys.backend_otp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedbackRequest {
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
}