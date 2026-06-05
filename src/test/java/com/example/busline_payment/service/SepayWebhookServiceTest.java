package com.example.busline_payment.service;

import com.example.busline_payment.config.SepayWebhookProperties;
import com.example.busline_payment.dto.WebhookHandlingResult;
import com.example.busline_payment.repository.BookingRepository;
import com.example.busline_payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SepayWebhookServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private BookingRepository bookingRepository;

	private SepayWebhookService service;
	private SepayWebhookProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SepayWebhookProperties();
		properties.setSecret("super-secret-key");
		properties.setAllowedClockSkewSeconds(300);
		properties.setPendingBookingStatus(0);
		properties.setPaidBookingStatus(1);
		service = new SepayWebhookService(
				transactionRepository,
				bookingRepository,
				properties,
				new ObjectMapper()
		);
	}

	@Test
	void rejectsInvalidSignature() {
		String body = """
			{"id":"txn-1","transferType":"in","transferAmount":1000,"code":"ORDER-1"}
			""";

		WebhookHandlingResult result = service.handleWebhook(
				body.getBytes(StandardCharsets.UTF_8),
				"sha256=invalid",
				Instant.now().getEpochSecond()
		);

		assertEquals(HttpStatus.UNAUTHORIZED, result.status());
		assertEquals(false, result.response().success());
		verify(transactionRepository, never()).insertIfAbsent(any(), anyString());
	}

	@Test
	void ignoresDuplicateTransaction() {
		String body = """
			{"id":"txn-2","transferType":"in","transferAmount":1500,"code":"BOOKING-2"}
			""";
		long timestamp = Instant.now().getEpochSecond();
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(false);

		WebhookHandlingResult result = service.handleWebhook(
				body.getBytes(StandardCharsets.UTF_8),
				sign(body, timestamp),
				timestamp
		);

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository, never()).markAsPaidIfPending(anyInt(), anyInt(), anyInt(), anyLong());
	}

	@Test
	void marksBookingPaidForFirstIncomingTransfer() {
		String body = """
			{"id":"txn-3","transferType":"in","transferAmount":2000,"code":"BOOKING-3"}
			""";
		long timestamp = Instant.now().getEpochSecond();
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(true);

		WebhookHandlingResult result = service.handleWebhook(
				body.getBytes(StandardCharsets.UTF_8),
				sign(body, timestamp),
				timestamp
		);

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository).markAsPaidIfPending(3, 0, 1, 2000L);
	}

	@Test
	void extractsBookingIdFromTransferContentWhenCodeIsMissing() {
		String body = """
			{"id":"txn-4","transferType":"in","transferAmount":2500,"content":"Thanh toan cho #45"}
			""";
		long timestamp = Instant.now().getEpochSecond();
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(true);

		WebhookHandlingResult result = service.handleWebhook(
				body.getBytes(StandardCharsets.UTF_8),
				sign(body, timestamp),
				timestamp
		);

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository).markAsPaidIfPending(45, 0, 1, 2500L);
	}

	private String sign(String body, long timestamp) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
			return "sha256=" + HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException(exception);
		}
	}

}
