package com.example.busline_payment.repository;

import com.example.busline_payment.dto.SepayWebhookPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

	private static final String INSERT_TRANSACTION_SQL = """
		INSERT INTO transactions
		(sepay_id, gateway, transaction_date, account_number, sub_account,
		 code, amount_in, amount_out, accumulated, content, reference_code, body)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		ON CONFLICT (sepay_id) DO NOTHING
		""";

	private final JdbcTemplate jdbcTemplate;

	public TransactionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean insertIfAbsent(SepayWebhookPayload payload, String body) {
		long transferAmount = payload.transferAmount() == null ? 0L : payload.transferAmount();
		boolean incomingTransfer = "in".equalsIgnoreCase(payload.transferType());
		boolean outgoingTransfer = "out".equalsIgnoreCase(payload.transferType());

		int updatedRows = jdbcTemplate.update(
				INSERT_TRANSACTION_SQL,
				payload.id(),
				payload.gateway(),
				payload.transactionDate(),
				payload.accountNumber(),
				defaultString(payload.subAccount()),
				payload.code(),
				incomingTransfer ? transferAmount : 0L,
				outgoingTransfer ? transferAmount : 0L,
				payload.accumulated() == null ? 0L : payload.accumulated(),
				payload.content(),
				defaultString(payload.referenceCode()),
				body
		);

		return updatedRows > 0;
	}

	private String defaultString(String value) {
		return value == null ? "" : value;
	}

}
