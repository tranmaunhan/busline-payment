package com.example.busline_payment.service;

import com.example.busline_payment.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class BookingStatusService {

    private final BookingRepository bookingRepository;

    public BookingStatusService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Optional<Integer> getBookingStatusByCode(String bookingCode) {
        if (!StringUtils.hasText(bookingCode)) {
            return Optional.empty();
        }

        return bookingRepository.findStatusByBookingCode(bookingCode.trim());
    }
}
