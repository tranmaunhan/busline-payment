package com.example.busline_payment.repository;

import com.example.busline_payment.dto.SepayWebhookPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class TransactionRepository {

    private static final DateTimeFormatter SEPAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String INSERT_TRANSACTION_SQL = """
    INSERT INTO transactions
    (sepay_id, gateway, transaction_date, account_number, sub_account,
     code, amount_in, amount_out, accumulated, content, reference_code, body)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
    ON CONFLICT (sepay_id) DO NOTHING
    """;
    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertIfAbsent(SepayWebhookPayload payload, String body) {
        long transferAmount = payload.transferAmount() == null ? 0L : payload.transferAmount();
        long accumulated = payload.accumulated() == null ? 0L : payload.accumulated();

        boolean incomingTransfer = "in".equalsIgnoreCase(payload.transferType());
        boolean outgoingTransfer = "out".equalsIgnoreCase(payload.transferType());

        int updatedRows = jdbcTemplate.update(
                INSERT_TRANSACTION_SQL,
                payload.id(),
                defaultString(payload.gateway()),
                parseTransactionDate(payload.transactionDate()),
                defaultString(payload.accountNumber()),
                defaultString(payload.subAccount()),
                defaultString(payload.code()),
                incomingTransfer ? transferAmount : 0L,
                outgoingTransfer ? transferAmount : 0L,
                accumulated,
                defaultString(payload.content()),
                defaultString(payload.referenceCode()),
                defaultString(body)
        );

        return updatedRows > 0;
    }

    private Timestamp parseTransactionDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        LocalDateTime localDateTime = LocalDateTime.parse(value, SEPAY_DATE_FORMATTER);
        return Timestamp.valueOf(localDateTime);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}