package com.byys.backend_otp.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficeBearerRepository extends JpaRepository<OfficeBearerApplication,Long> {
    Page<OfficeBearerApplication> findByApprovedTrue(Pageable pageable);

    Page<OfficeBearerApplication> findByApprovedFalse(Pageable pageable);

    boolean existsByUserAndApprovedFalse(User user);

    boolean existsByUserAndApprovedTrue(User user);

    Optional<OfficeBearerApplication> findByUser(User user);

    Optional<OfficeBearerApplication> findByUserAndApprovedTrue(User user);
}

