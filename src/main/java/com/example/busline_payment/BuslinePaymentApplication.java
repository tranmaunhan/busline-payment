package com.example.busline_payment;

import com.example.busline_payment.config.SepayWebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SepayWebhookProperties.class)
public class BuslinePaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuslinePaymentApplication.class, args);
	}

}
