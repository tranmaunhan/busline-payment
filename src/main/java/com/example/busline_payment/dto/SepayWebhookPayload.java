package com.example.busline_payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SepayWebhookPayload(
		Long id,
		String gateway,
		String transactionDate,
		String accountNumber,
		String subAccount,
		String code,
		Long transferAmount,
		String transferType,
		Long accumulated,
		String content,
		String referenceCode
) {
}
