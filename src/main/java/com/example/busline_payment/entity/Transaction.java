package com.example.busline_payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sepay_id", unique = true)
    private Long sepayId;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "sub_account")
    private String subAccount;

    @Column(name = "code")
    private String code;

    @Column(name = "amount_in")
    private Long amountIn;

    @Column(name = "amount_out")
    private Long amountOut;

    @Column(name = "accumulated")
    private Long accumulated;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "body", columnDefinition = "jsonb")
    private String body;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}