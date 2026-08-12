package com.travel.insurance.icd11;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Icd11CodeRepository extends JpaRepository<Icd11Code, UUID> {

    Optional<Icd11Code> findByCode(String code);

    Page<Icd11Code> findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String code, String title, Pageable pageable);

    Page<Icd11Code> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
