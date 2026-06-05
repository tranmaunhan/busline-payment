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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SepayWebhookService {

	private static final Pattern LABELED_BOOKING_ID_PATTERN = Pattern.compile("(?i)booking\\s*[-_:#= ]*\\s*(\\d+)");
	private static final Pattern HASH_BOOKING_ID_PATTERN = Pattern.compile("#(\\d+)");
	private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("^\\d+$");

	private final TransactionRepository transactionRepository;
	private final BookingRepository bookingRepository;
	private final SepayWebhookProperties properties;
	private final ObjectMapper objectMapper;

	public SepayWebhookService(
			TransactionRepository transactionRepository,
			BookingRepository bookingRepository,
			SepayWebhookProperties properties,
			ObjectMapper objectMapper
	) {
		this.transactionRepository = transactionRepository;
		this.bookingRepository = bookingRepository;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public WebhookHandlingResult handleWebhook(byte[] rawBody, String signature, long timestamp) {
		String body = rawBody == null ? "" : new String(rawBody, StandardCharsets.UTF_8);
		if (!StringUtils.hasText(body)) {
			return new WebhookHandlingResult(HttpStatus.BAD_REQUEST, WebhookResponse.error("Empty body"));
		}

		if (!StringUtils.hasText(properties.getSecret())) {
			return new WebhookHandlingResult(
					HttpStatus.INTERNAL_SERVER_ERROR,
					WebhookResponse.error("Webhook secret is not configured")
			);
		}

		if (isExpired(timestamp)) {
			return new WebhookHandlingResult(HttpStatus.UNAUTHORIZED, WebhookResponse.error("Request expired"));
		}

		if (!hasValidSignature(signature, timestamp, body)) {
			return new WebhookHandlingResult(HttpStatus.UNAUTHORIZED, WebhookResponse.error("Invalid signature"));
		}

		SepayWebhookPayload payload = parsePayload(body);
		if (payload == null || !StringUtils.hasText(payload.id())) {
			return new WebhookHandlingResult(HttpStatus.BAD_REQUEST, WebhookResponse.error("Invalid payload"));
		}

		boolean inserted = transactionRepository.insertIfAbsent(payload, body);
		if (!inserted) {
			return new WebhookHandlingResult(HttpStatus.OK, WebhookResponse.ok());
		}

		if ("in".equalsIgnoreCase(payload.transferType())) {
			Integer bookingId = extractBookingId(payload);
			if (bookingId != null) {
				bookingRepository.markAsPaidIfPending(
						bookingId,
						properties.getPendingBookingStatus(),
						properties.getPaidBookingStatus(),
						payload.transferAmount() == null ? 0L : payload.transferAmount()
				);
			}
		}

		return new WebhookHandlingResult(HttpStatus.OK, WebhookResponse.ok());
	}

	private Integer extractBookingId(SepayWebhookPayload payload) {
		Integer fromCode = extractBookingId(payload.code());
		if (fromCode != null) {
			return fromCode;
		}

		Integer fromReferenceCode = extractBookingId(payload.referenceCode());
		if (fromReferenceCode != null) {
			return fromReferenceCode;
		}

		return extractBookingId(payload.content());
	}

	private SepayWebhookPayload parsePayload(String body) {
		try {
			return objectMapper.readValue(body, SepayWebhookPayload.class);
		}
		catch (JsonProcessingException exception) {
			return null;
		}
	}

	private boolean isExpired(long timestamp) {
		long now = Instant.now().getEpochSecond();
		return Math.abs(now - timestamp) > properties.getAllowedClockSkewSeconds();
	}

	private boolean hasValidSignature(String signature, long timestamp, String body) {
		String expected = "sha256=" + signPayload(properties.getSecret(), timestamp + "." + body);
		byte[] actualBytes = signature == null ? new byte[0] : signature.getBytes(StandardCharsets.UTF_8);
		byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
		return actualBytes.length == expectedBytes.length && MessageDigest.isEqual(actualBytes, expectedBytes);
	}

	private Integer extractBookingId(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		String normalized = value.trim();
		if (DIGITS_ONLY_PATTERN.matcher(normalized).matches()) {
			return parseBookingId(normalized);
		}

		Integer labeled = extractFirstMatch(LABELED_BOOKING_ID_PATTERN, normalized);
		if (labeled != null) {
			return labeled;
		}

		return extractFirstMatch(HASH_BOOKING_ID_PATTERN, normalized);
	}

	private Integer extractFirstMatch(Pattern pattern, String value) {
		Matcher matcher = pattern.matcher(value);
		if (!matcher.find()) {
			return null;
		}

		return parseBookingId(matcher.group(1));
	}

	private Integer parseBookingId(String rawBookingId) {
		try {
			return Integer.valueOf(rawBookingId);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private String signPayload(String secret, String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("Unable to calculate SePay HMAC signature", exception);
		}
	}

}
