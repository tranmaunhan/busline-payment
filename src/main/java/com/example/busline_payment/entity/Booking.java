package com.example.busline_payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"Bookings\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @Column(name = "\"Id\"")
    private Integer id;

    @Column(name = "\"UserId\"")
    private Integer userId;

    @Column(name = "\"BookingTime\"")
    private LocalDateTime bookingTime;

    @Column(name = "\"Status\"")
    private Integer status;

    @Column(name = "\"TotalAmount\"")
    private Long totalAmount;

    @Column(name = "\"BookingCode\"")
    private String bookingCode;
}