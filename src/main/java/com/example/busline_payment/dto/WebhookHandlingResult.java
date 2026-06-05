package com.example.busline_payment.dto;

import org.springframework.http.HttpStatus;

public record WebhookHandlingResult(HttpStatus status, WebhookResponse response) {
}
