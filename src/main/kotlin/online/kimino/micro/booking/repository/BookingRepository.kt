package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BookingRepository : JpaRepository<Booking, Long> {
    fun findAllByUserId(userId: Long): List<Booking>

    fun findAllByServiceProviderId(providerId: Long): List<Booking>

    fun findAllByServiceId(serviceId: Long): List<Booking>

    @Query("SELECT b FROM Booking b WHERE b.service.provider.id = :providerId AND b.status = :status")
    fun findAllByProviderIdAndStatus(providerId: Long, status: BookingStatus): List<Booking>

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    fun findAllByUserIdAndStatus(userId: Long, status: BookingStatus): List<Booking>

    @Query("SELECT b FROM Booking b WHERE b.service.id = :serviceId AND b.status IN ('CONFIRMED', 'PENDING') AND ((b.startTime <= :endTime AND b.endTime >= :startTime))")
    fun findOverlappingBookings(serviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): List<Booking>

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.startTime BETWEEN :start AND :end")
    fun findUpcomingBookings(start: LocalDateTime, end: LocalDateTime): List<Booking>
}