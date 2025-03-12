package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.CyclicBooking
import online.kimino.micro.booking.entity.CyclicBookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CyclicBookingRepository : JpaRepository<CyclicBooking, Long> {
    fun findAllByUserId(userId: Long): List<CyclicBooking>

    @Query("SELECT cb FROM CyclicBooking cb WHERE cb.service.provider.id = :providerId")
    fun findAllByServiceProviderId(providerId: Long): List<CyclicBooking>

    fun findAllByServiceId(serviceId: Long): List<CyclicBooking>

    fun findAllByUserIdAndStatus(userId: Long, status: CyclicBookingStatus): List<CyclicBooking>

    @Query("SELECT cb FROM CyclicBooking cb WHERE cb.service.provider.id = :providerId AND cb.status = :status")
    fun findAllByServiceProviderIdAndStatus(providerId: Long, status: CyclicBookingStatus): List<CyclicBooking>

    /**
     * Find cyclic bookings for a specific service that could overlap with a given time slot
     * This query finds cyclic bookings that:
     * 1. Are for the requested service
     * 2. Are CONFIRMED
     * 3. Start date is before or on the requested date
     * 4. End date is null or after or on the requested date
     */
    @Query(
        "SELECT cb FROM CyclicBooking cb WHERE cb.service.id = :serviceId " +
                "AND cb.status = 'CONFIRMED' " +
                "AND cb.startDate <= :date " +
                "AND (cb.endDate IS NULL OR cb.endDate >= :date)"
    )
    fun findPotentialOverlappingCyclicBookings(
        serviceId: Long,
        date: java.time.LocalDate
    ): List<CyclicBooking>
}