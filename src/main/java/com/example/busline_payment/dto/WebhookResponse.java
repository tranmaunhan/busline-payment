package com.example.busline_payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookResponse(boolean success, String message) {

	public static WebhookResponse ok() {
		return new WebhookResponse(true, null);
	}

	public static WebhookResponse error(String message) {
		return new WebhookResponse(false, message);
	}

}
