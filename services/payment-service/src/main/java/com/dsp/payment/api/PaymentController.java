package com.dsp.payment.api;

import com.dsp.payment.repo.PaymentRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRecordRepository repository;

    public PaymentController(PaymentRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{orderId}")
    public PaymentResponse get(@PathVariable String orderId) {
        return repository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for order " + orderId));
    }
}
