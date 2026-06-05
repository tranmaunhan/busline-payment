package com.example.busline_payment.controller;

import com.example.busline_payment.dto.WebhookHandlingResult;
import com.example.busline_payment.dto.WebhookResponse;
import com.example.busline_payment.service.SepayWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SepayWebhookController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SepayWebhookController.class);

	private final SepayWebhookService sepayWebhookService;

	public SepayWebhookController(SepayWebhookService sepayWebhookService) {
		this.sepayWebhookService = sepayWebhookService;
	}

	@PostMapping(path = "/webhook/sepay", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<WebhookResponse> handleWebhook(
			@RequestBody(required = false) byte[] rawBody,
			@RequestHeader(name = "x-sepay-signature", required = false) String signature,
			@RequestHeader(name = "x-sepay-timestamp", required = false) String timestampHeader
	) {
		try {
			WebhookHandlingResult result = sepayWebhookService.handleWebhook(
					rawBody,
					signature,
					parseTimestamp(timestampHeader)
			);
			return ResponseEntity.status(result.status()).body(result.response());
		}
		catch (Exception exception) {
			LOGGER.error("SePay webhook error", exception);
			return ResponseEntity.internalServerError().body(WebhookResponse.error("Internal error"));
		}
	}

	private long parseTimestamp(String timestampHeader) {
		if (timestampHeader == null || timestampHeader.isBlank()) {
			return 0L;
		}

		try {
			return Long.parseLong(timestampHeader);
		}
		catch (NumberFormatException exception) {
			return 0L;
		}
	}

}
