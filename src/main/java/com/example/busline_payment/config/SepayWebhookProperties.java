package com.example.busline_payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sepay.webhook")
public class SepayWebhookProperties {

	private String secret;
	private long allowedClockSkewSeconds = 300;
	private int pendingBookingStatus = 0;
	private int paidBookingStatus = 1;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getAllowedClockSkewSeconds() {
		return allowedClockSkewSeconds;
	}

	public void setAllowedClockSkewSeconds(long allowedClockSkewSeconds) {
		this.allowedClockSkewSeconds = allowedClockSkewSeconds;
	}

	public int getPendingBookingStatus() {
		return pendingBookingStatus;
	}

	public void setPendingBookingStatus(int pendingBookingStatus) {
		this.pendingBookingStatus = pendingBookingStatus;
	}

	public int getPaidBookingStatus() {
		return paidBookingStatus;
	}

	public void setPaidBookingStatus(int paidBookingStatus) {
		this.paidBookingStatus = paidBookingStatus;
	}

}
