package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.Availability
import online.kimino.micro.booking.repository.AvailabilityRepository
import online.kimino.micro.booking.repository.BookingRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class AvailabilityService(
    private val availabilityRepository: AvailabilityRepository,
    private val bookingRepository: BookingRepository
) {
    @Value("\${app.booking.default-slot-duration:60}")
    private val defaultSlotDuration: Int = 60

    @Value("\${app.booking.max-advance-days:60}")
    private val maxAdvanceDays: Int = 60

    fun findById(id: Long): Optional<Availability> {
        return availabilityRepository.findById(id)
    }

    fun findAllByServiceId(serviceId: Long): List<Availability> {
        return availabilityRepository.findAllByServiceId(serviceId)
    }

    @Transactional
    fun createAvailability(availability: Availability): Availability {
        if (!availability.isValid()) {
            throw IllegalArgumentException("End time must be after start time")
        }
        return availabilityRepository.save(availability)
    }

    @Transactional
    fun updateAvailability(availability: Availability): Availability {
        if (!availability.isValid()) {
            throw IllegalArgumentException("End time must be after start time")
        }
        return availabilityRepository.save(availability)
    }

    @Transactional
    fun deleteAvailability(id: Long) {
        availabilityRepository.deleteById(id)
    }

    /**
     * Get available time slots for a specific service and date
     */
    fun getAvailableTimeSlots(serviceId: Long, date: LocalDate): List<Map<String, LocalDateTime>> {
        val availabilities = availabilityRepository.findAllByServiceIdAndDayOfWeek(serviceId, date.dayOfWeek)

        // If no availabilities defined for this day, return empty list
        if (availabilities.isEmpty()) {
            return emptyList()
        }

        // Get service duration or use default
        val serviceDuration = availabilityRepository.findById(serviceId)
            .map { it.service.duration }
            .orElse(defaultSlotDuration)

        // Get all existing bookings for this service on this date
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.plusDays(1).atStartOfDay()

        val existingBookings = bookingRepository.findOverlappingBookings(
            serviceId,
            startOfDay,
            endOfDay
        )

        // Generate available time slots
        val availableSlots = mutableListOf<Map<String, LocalDateTime>>()

        for (availability in availabilities) {
            var currentSlotStart = LocalDateTime.of(date, availability.startTime)
            val endTime = LocalDateTime.of(date, availability.endTime)

            while (currentSlotStart.plusMinutes(serviceDuration.toLong()) <= endTime) {
                val currentSlotEnd = currentSlotStart.plusMinutes(serviceDuration.toLong())

                // Check if this slot overlaps with any existing booking
                val isAvailable = existingBookings.none { booking ->
                    (currentSlotStart.isBefore(booking.endTime) && currentSlotEnd.isAfter(booking.startTime))
                }

                if (isAvailable && currentSlotStart.isAfter(LocalDateTime.now())) {
                    availableSlots.add(
                        mapOf(
                            "start" to currentSlotStart,
                            "end" to currentSlotEnd
                        )
                    )
                }

                // Move to next slot
                currentSlotStart = currentSlotStart.plusMinutes(serviceDuration.toLong())
            }
        }

        return availableSlots
    }

    /**
     * Get available dates for a specific service within the next X days
     */
    fun getAvailableDates(serviceId: Long): List<LocalDate> {
        val now = LocalDate.now()
        val endDate = now.plusDays(maxAdvanceDays.toLong())

        // Get all days of week for which this service has availability defined
        val availabilities = availabilityRepository.findAllByServiceId(serviceId)
        val daysWithAvailability = availabilities.map { it.dayOfWeek }.distinct()

        if (daysWithAvailability.isEmpty()) {
            return emptyList()
        }

        // Generate all dates within range that have availability defined
        return (0..maxAdvanceDays.toLong())
            .map { now.plusDays(it) }
            .filter { date -> daysWithAvailability.contains(date.dayOfWeek) }
    }

    /**
     * Check if a specific time slot is available
     */
    fun isTimeSlotAvailable(serviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        // Check if the service has availability defined for this day of week
        val dayOfWeek = startTime.dayOfWeek
        val availabilities = availabilityRepository.findAllByServiceIdAndDayOfWeek(serviceId, dayOfWeek)

        if (availabilities.isEmpty()) {
            return false
        }

        // Check if the requested time slot fits within defined availability
        val timeSlotFitsAvailability = availabilities.any { availability ->
            val availabilityStart = LocalDateTime.of(startTime.toLocalDate(), availability.startTime)
            val availabilityEnd = LocalDateTime.of(startTime.toLocalDate(), availability.endTime)

            startTime >= availabilityStart && endTime <= availabilityEnd
        }

        if (!timeSlotFitsAvailability) {
            return false
        }

        // Check for conflicting bookings
        val overlappingBookings = bookingRepository.findOverlappingBookings(
            serviceId,
            startTime,
            endTime
        )

        return overlappingBookings.isEmpty()
    }
}