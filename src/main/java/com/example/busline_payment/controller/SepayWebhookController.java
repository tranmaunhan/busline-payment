package com.example.busline_payment.controller;

import com.example.busline_payment.dto.WebhookHandlingResult;
import com.example.busline_payment.dto.WebhookResponse;
import com.example.busline_payment.service.SepayWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SepayWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SepayWebhookController.class);

    private final SepayWebhookService sepayWebhookService;

    public SepayWebhookController(SepayWebhookService sepayWebhookService) {
        this.sepayWebhookService = sepayWebhookService;
    }

    @PostMapping(
            path = "/webhook/sepay",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebhookResponse> handleWebhook(
            @RequestBody(required = false) byte[] rawBody,
            @RequestHeader(name = "x-sepay-signature", required = false) String signature,
            @RequestHeader(name = "x-sepay-timestamp", required = false) String timestampHeader,
            HttpServletRequest request
    ) {
        long parsedTimestamp = parseTimestamp(timestampHeader);

        LOGGER.info(
                "SePay webhook received: method={}, path={}, clientIp={}, forwardedFor={}, hasSignature={}, signaturePreview={}, hasTimestampHeader={}, rawTimestampHeader={}, parsedTimestamp={}, contentType={}, userAgent={}, bodyBytes={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getHeader("X-Forwarded-For"),
                signature != null && !signature.isBlank(),
                maskSignature(signature),
                timestampHeader != null && !timestampHeader.isBlank(),
                timestampHeader,
                parsedTimestamp,
                request.getContentType(),
                request.getHeader("User-Agent"),
                rawBody == null ? 0 : rawBody.length
        );

        try {
            WebhookHandlingResult result = sepayWebhookService.handleWebhook(
                    rawBody,
                    signature,
                    parsedTimestamp
            );

            if (result.status().is2xxSuccessful()) {
                LOGGER.info(
                        "SePay webhook handled successfully: status={}, message={}",
                        result.status().value(),
                        result.response().message()
                );
            } else {
                LOGGER.warn(
                        "SePay webhook handled with non-success status: status={}, message={}",
                        result.status().value(),
                        result.response().message()
                );
            }

            return ResponseEntity
                    .status(result.status())
                    .body(result.response());

        } catch (Exception exception) {
            LOGGER.error("SePay webhook error", exception);

            return ResponseEntity
                    .internalServerError()
                    .body(WebhookResponse.error("Internal error"));
        }
    }

    private long parseTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(timestampHeader);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid SePay timestamp header: {}", timestampHeader);
            return 0L;
        }
    }

    private String maskSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return "missing";
        }

        if (signature.length() <= 16) {
            return signature;
        }

        return signature.substring(0, 12) + "...";
    }
}
