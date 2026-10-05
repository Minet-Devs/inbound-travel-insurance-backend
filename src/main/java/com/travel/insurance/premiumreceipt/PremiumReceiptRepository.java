package com.travel.insurance.premiumreceipt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PremiumReceiptRepository extends JpaRepository<PremiumReceipt, UUID> {
}
