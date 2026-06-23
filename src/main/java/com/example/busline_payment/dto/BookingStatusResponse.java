package com.example.busline_payment.dto;

public record BookingStatusResponse(
        boolean success,
        String message,
        String bookingCode,
        Integer status
) {
    public static BookingStatusResponse found(String bookingCode, Integer status) {
        return new BookingStatusResponse(true, "Booking status retrieved successfully", bookingCode, status);
    }

    public static BookingStatusResponse notFound(String bookingCode) {
        return new BookingStatusResponse(false, "Booking not found", bookingCode, null);
    }
}
