package com.example.busline_payment.service;

import com.example.busline_payment.config.SepayWebhookProperties;
import com.example.busline_payment.dto.SepayWebhookPayload;
import com.example.busline_payment.dto.WebhookHandlingResult;
import com.example.busline_payment.dto.WebhookResponse;
import com.example.busline_payment.repository.BookingRepository;
import com.example.busline_payment.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SepayWebhookService {

	private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("(?i)(SAIGONSTBK[A-Z0-9]+)");

	private final TransactionRepository transactionRepository;
	private final BookingRepository bookingRepository;
	private final SepayWebhookProperties properties;
	private final ObjectMapper objectMapper;

	public SepayWebhookService(
			TransactionRepository transactionRepository,
			BookingRepository bookingRepository,
			SepayWebhookProperties properties,
			ObjectMapper objectMapper) {
		this.transactionRepository = transactionRepository;
		this.bookingRepository = bookingRepository;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public WebhookHandlingResult handleWebhook(byte[] rawBody) {
		String body = rawBody == null
				? ""
				: new String(rawBody, StandardCharsets.UTF_8);

		if (!StringUtils.hasText(body)) {
			return new WebhookHandlingResult(
					HttpStatus.BAD_REQUEST,
					WebhookResponse.error("Empty body"));
		}

		SepayWebhookPayload payload = parsePayload(body);

		if (payload == null || payload.id() == null) {
			return new WebhookHandlingResult(
					HttpStatus.BAD_REQUEST,
					WebhookResponse.error("Invalid payload"));
		}

		boolean inserted = transactionRepository.insertIfAbsent(payload, body);

		if (!inserted) {
			return new WebhookHandlingResult(
					HttpStatus.OK,
					WebhookResponse.ok());
		}

		if (!"in".equalsIgnoreCase(payload.transferType())) {
			return new WebhookHandlingResult(
					HttpStatus.OK,
					WebhookResponse.ok());
		}

		String bookingCode = extractBookingCode(payload);

		if (!StringUtils.hasText(bookingCode)) {
			return new WebhookHandlingResult(
					HttpStatus.OK,
					WebhookResponse.ok());
		}

		long transferAmount = payload.transferAmount() == null
				? 0L
				: payload.transferAmount();

		int updatedRows = bookingRepository.markAsPaidByBookingCodeIfPending(
				bookingCode,
				properties.getPendingBookingStatus(),
				properties.getPaidBookingStatus(),
				transferAmount);

		System.out.println("Webhook matched bookingCode = " + bookingCode
				+ ", amount = " + transferAmount
				+ ", updatedRows = " + updatedRows);

		return new WebhookHandlingResult(
				HttpStatus.OK,
				WebhookResponse.ok());
	}

	private SepayWebhookPayload parsePayload(String body) {
		try {
			return objectMapper.readValue(body, SepayWebhookPayload.class);
		} catch (JsonProcessingException exception) {
			return null;
		}
	}

	private String extractBookingCode(SepayWebhookPayload payload) {
		String fromCode = extractBookingCode(payload.code());
		if (StringUtils.hasText(fromCode)) {
			return fromCode;
		}

		String fromReferenceCode = extractBookingCode(payload.referenceCode());
		if (StringUtils.hasText(fromReferenceCode)) {
			return fromReferenceCode;
		}

		return extractBookingCode(payload.content());
	}

	private String extractBookingCode(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		Matcher matcher = BOOKING_CODE_PATTERN.matcher(value);

		if (!matcher.find()) {
			return null;
		}

		return matcher.group(1).toUpperCase();
	}
}