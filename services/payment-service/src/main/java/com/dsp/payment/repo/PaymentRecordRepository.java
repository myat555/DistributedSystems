package com.dsp.payment.repo;

import com.dsp.payment.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String> {
    Optional<PaymentRecord> findByOrderId(String orderId);
}
