package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.*
import online.kimino.micro.booking.repository.BookingRepository
import online.kimino.micro.booking.repository.CyclicBookingRepository
import online.kimino.micro.booking.repository.ServiceRepository
import online.kimino.micro.booking.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Service
class CyclicBookingService(
    private val cyclicBookingRepository: CyclicBookingRepository,
    private val bookingRepository: BookingRepository,
    private val serviceRepository: ServiceRepository,
    private val userRepository: UserRepository,
    private val availabilityService: AvailabilityService,
    private val notificationService: NotificationService
) {
    @Transactional
    fun createCyclicBooking(
        serviceId: Long,
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate?,
        recurrencePattern: RecurrencePattern,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?,
        startTime: LocalTime,
        endTime: LocalTime,
        notes: String?
    ): CyclicBooking {
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { NoSuchElementException("Service not found with id $serviceId") }

        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found with id $userId") }

        // Validate recurrence pattern parameters
        validateRecurrencePattern(recurrencePattern, dayOfWeek, dayOfMonth)

        // Create cyclic booking
        val cyclicBooking = CyclicBooking(
            startDate = startDate,
            endDate = endDate,
            recurrencePattern = recurrencePattern,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            startTime = startTime,
            endTime = endTime,
            user = user,
            service = service,
            notes = notes
        )

        val savedCyclicBooking = cyclicBookingRepository.save(cyclicBooking)

        // Generate initial bookings based on the recurrence pattern
        generateBookings(savedCyclicBooking)

        return savedCyclicBooking
    }

    private fun validateRecurrencePattern(
        recurrencePattern: RecurrencePattern,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?
    ) {
        when (recurrencePattern) {
            RecurrencePattern.WEEKLY -> if (dayOfWeek == null) {
                throw IllegalArgumentException("Day of week is required for weekly recurrence")
            }

            RecurrencePattern.MONTHLY -> if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
                throw IllegalArgumentException("Valid day of month (1-31) is required for monthly recurrence")
            }
        }
    }

    @Transactional
    fun updateCyclicBookingStatus(id: Long, status: CyclicBookingStatus): CyclicBooking {
        val cyclicBooking = cyclicBookingRepository.findById(id)
            .orElseThrow { NoSuchElementException("Cyclic booking not found with id $id") }

        cyclicBooking.status = status

        // Update status of all pending bookings associated with this cyclic booking
        val bookingsToUpdate = cyclicBooking.bookings.filter { it.status == BookingStatus.PENDING }
        for (booking in bookingsToUpdate) {
            booking.status = when (status) {
                CyclicBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
                CyclicBookingStatus.CANCELLED -> BookingStatus.CANCELLED
                else -> booking.status // Keep as is for PENDING
            }

            // Create notifications for confirmed/cancelled bookings
            if (status == CyclicBookingStatus.CONFIRMED) {
                notificationService.createBookingNotification(
                    booking,
                    "Booking Confirmation - Cyclic",
                    "Your recurring booking for ${booking.service.name} has been confirmed.",
                    NotificationType.EMAIL
                )
            } else if (status == CyclicBookingStatus.CANCELLED) {
                notificationService.createBookingNotification(
                    booking,
                    "Booking Cancellation - Cyclic",
                    "Your recurring booking for ${booking.service.name} has been cancelled.",
                    NotificationType.EMAIL
                )
            }
        }

        return cyclicBookingRepository.save(cyclicBooking)
    }

    @Transactional
    fun generateBookings(cyclicBooking: CyclicBooking) {
        val startDate = cyclicBooking.startDate
        val endDate = cyclicBooking.endDate ?: startDate.plusMonths(3) // Default to 3 months if no end date

        val bookingDates = generateBookingDates(
            startDate,
            endDate,
            cyclicBooking.recurrencePattern,
            cyclicBooking.dayOfWeek,
            cyclicBooking.dayOfMonth
        )

        for (date in bookingDates) {
            // Check if the time slot is available
            val startDateTime = LocalDateTime.of(date, cyclicBooking.startTime)
            val endDateTime = LocalDateTime.of(date, cyclicBooking.endTime)

            if (availabilityService.isTimeSlotAvailable(cyclicBooking.service.id, startDateTime, endDateTime)) {
                val booking = Booking(
                    startTime = startDateTime,
                    endTime = endDateTime,
                    status = BookingStatus.PENDING, // All generated bookings start as pending
                    user = cyclicBooking.user,
                    service = cyclicBooking.service,
                    notes = cyclicBooking.notes,
                    cyclicBooking = cyclicBooking
                )

                bookingRepository.save(booking)
                cyclicBooking.bookings.add(booking)
            }
        }
    }

    private fun generateBookingDates(
        startDate: LocalDate,
        endDate: LocalDate,
        recurrencePattern: RecurrencePattern,
        dayOfWeek: DayOfWeek?,
        dayOfMonth: Int?
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            when (recurrencePattern) {
                RecurrencePattern.WEEKLY -> {
                    if (currentDate.dayOfWeek == dayOfWeek) {
                        dates.add(currentDate)
                    }
                    currentDate = currentDate.plusDays(1)
                }

                RecurrencePattern.MONTHLY -> {
                    if (dayOfMonth != null && currentDate.dayOfMonth == dayOfMonth) {
                        dates.add(currentDate)
                        // Move to the next month
                        currentDate = currentDate.plusMonths(1).withDayOfMonth(1)
                    } else {
                        // If we're past the target day this month, move to the 1st of next month
                        if (dayOfMonth != null && currentDate.dayOfMonth > dayOfMonth) {
                            currentDate = currentDate.plusMonths(1).withDayOfMonth(1)
                        } else {
                            // Otherwise just move to the next day
                            currentDate = currentDate.plusDays(1)
                        }
                    }
                }
            }
        }

        return dates
    }

    @Transactional
    fun updateCyclicBookingNotes(id: Long, notes: String): CyclicBooking {
        val cyclicBooking = cyclicBookingRepository.findById(id)
            .orElseThrow { NoSuchElementException("Cyclic booking not found with id $id") }

        cyclicBooking.notes = notes

        // Update notes for all associated bookings
        for (booking in cyclicBooking.bookings) {
            booking.notes = notes
            bookingRepository.save(booking)
        }

        return cyclicBookingRepository.save(cyclicBooking)
    }

    @Transactional
    fun deleteCyclicBooking(id: Long) {
        cyclicBookingRepository.deleteById(id)
    }

    fun findById(id: Long): Optional<CyclicBooking> {
        return cyclicBookingRepository.findById(id)
    }

    fun findAllByUserId(userId: Long): List<CyclicBooking> {
        return cyclicBookingRepository.findAllByUserId(userId)
    }

    fun findAllByProviderId(providerId: Long): List<CyclicBooking> {
        return cyclicBookingRepository.findAllByServiceProviderId(providerId)
    }

    fun findAllByUserIdAndStatus(userId: Long, status: CyclicBookingStatus): List<CyclicBooking> {
        return cyclicBookingRepository.findAllByUserIdAndStatus(userId, status)
    }

    fun findAllByProviderIdAndStatus(providerId: Long, status: CyclicBookingStatus): List<CyclicBooking> {
        return cyclicBookingRepository.findAllByServiceProviderIdAndStatus(providerId, status)
    }
}