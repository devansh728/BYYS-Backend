package com.byys.backend_otp.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OfficeBearerApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String district;
    private String state;
    private String contactDetails;
    private String socialWorkDescription;
    private LocalDateTime appliedAt;
    private Boolean approved = false;
    private LocalDateTime approvedAt;
}
