package com.example.busline_payment.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {

    private static final String MARK_BOOKING_PAID_BY_CODE_SQL = """
        UPDATE "Bookings"
        SET "Status" = ?
        WHERE "BookingCode" = ?
          AND "Status" = ?
          AND "TotalAmount" = ?
        """;

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int markAsPaidByBookingCodeIfPending(
            String bookingCode,
            int pendingStatus,
            int paidStatus,
            long transferAmount
    ) {
        return jdbcTemplate.update(
                MARK_BOOKING_PAID_BY_CODE_SQL,
                paidStatus,
                bookingCode,
                pendingStatus,
                transferAmount
        );
    }
}