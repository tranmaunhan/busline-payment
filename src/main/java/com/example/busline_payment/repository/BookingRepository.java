package com.example.busline_payment.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {

	private static final String MARK_BOOKING_PAID_SQL = """
		UPDATE "Bookings"
		SET "Status" = ?
		WHERE "Id" = ? AND "Status" = ? AND "TotalAmount" <= ?
		""";

	private final JdbcTemplate jdbcTemplate;

	public BookingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int markAsPaidIfPending(int bookingId, int pendingStatus, int paidStatus, long transferAmount) {
		return jdbcTemplate.update(
				MARK_BOOKING_PAID_SQL,
				paidStatus,
				bookingId,
				pendingStatus,
				transferAmount
		);
	}

}
