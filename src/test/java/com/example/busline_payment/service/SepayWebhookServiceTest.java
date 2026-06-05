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

import java.nio.charset.StandardCharsets;

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
	void rejectsInvalidPayload() {
		String body = """
			{}
			""";

		WebhookHandlingResult result = service.handleWebhook(body.getBytes(StandardCharsets.UTF_8));

		assertEquals(HttpStatus.BAD_REQUEST, result.status());
		assertEquals(false, result.response().success());
		verify(transactionRepository, never()).insertIfAbsent(any(), anyString());
	}

	@Test
	void ignoresDuplicateTransaction() {
		String body = """
			{"id":"txn-2","transferType":"in","transferAmount":1500,"code":"BOOKING-2"}
			""";
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(false);

		WebhookHandlingResult result = service.handleWebhook(body.getBytes(StandardCharsets.UTF_8));

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository, never()).markAsPaidIfPending(anyInt(), anyInt(), anyInt(), anyLong());
	}

	@Test
	void marksBookingPaidForFirstIncomingTransfer() {
		String body = """
			{"id":"txn-3","transferType":"in","transferAmount":2000,"code":"BOOKING-3"}
			""";
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(true);

		WebhookHandlingResult result = service.handleWebhook(body.getBytes(StandardCharsets.UTF_8));

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository).markAsPaidIfPending(3, 0, 1, 2000L);
	}

	@Test
	void extractsBookingIdFromTransferContentWhenCodeIsMissing() {
		String body = """
			{"id":"txn-4","transferType":"in","transferAmount":2500,"content":"Thanh toan cho #45"}
			""";
		when(transactionRepository.insertIfAbsent(any(), anyString())).thenReturn(true);

		WebhookHandlingResult result = service.handleWebhook(body.getBytes(StandardCharsets.UTF_8));

		assertEquals(HttpStatus.OK, result.status());
		assertEquals(true, result.response().success());
		verify(bookingRepository).markAsPaidIfPending(45, 0, 1, 2500L);
	}

}
