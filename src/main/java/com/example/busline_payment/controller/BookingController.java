package com.example.busline_payment.controller;

import com.example.busline_payment.dto.BookingStatusResponse;
import com.example.busline_payment.service.BookingStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingStatusService bookingStatusService;

    public BookingController(BookingStatusService bookingStatusService) {
        this.bookingStatusService = bookingStatusService;
    }

    @GetMapping("/{bookingCode}/status")
    public ResponseEntity<BookingStatusResponse> getBookingStatus(
            @PathVariable String bookingCode
    ) {
        if (!StringUtils.hasText(bookingCode)) {
            return ResponseEntity.badRequest()
                    .body(BookingStatusResponse.notFound(bookingCode));
        }

        Optional<Integer> status = bookingStatusService.getBookingStatusByCode(bookingCode);

        if (status.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(BookingStatusResponse.notFound(bookingCode));
        }

        return ResponseEntity.ok(BookingStatusResponse.found(bookingCode, status.get()));
    }
}
