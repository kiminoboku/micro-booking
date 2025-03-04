package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.Notification
import online.kimino.micro.booking.entity.NotificationType
import online.kimino.micro.booking.exception.BookingException
import online.kimino.micro.booking.repository.BookingRepository
import online.kimino.micro.booking.repository.NotificationRepository
import online.kimino.micro.booking.repository.ServiceRepository
import online.kimino.micro.booking.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val serviceRepository: ServiceRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val availabilityService: AvailabilityService,
    private val emailService: EmailService
) {
    fun findById(id: Long): Optional<Booking> {
        return bookingRepository.findById(id)
    }

    fun findAllByUserId(userId: Long): List<Booking> {
        return bookingRepository.findAllByUserId(userId)
    }

    fun findAllByProviderId(providerId: Long): List<Booking> {
        return bookingRepository.findAllByServiceProviderId(providerId)
    }

    fun findAllByServiceId(serviceId: Long): List<Booking> {
        return bookingRepository.findAllByServiceId(serviceId)
    }

    fun findAllByUserIdAndStatus(userId: Long, status: BookingStatus): List<Booking> {
        return bookingRepository.findAllByUserIdAndStatus(userId, status)
    }

    fun findAllByProviderIdAndStatus(providerId: Long, status: BookingStatus): List<Booking> {
        return bookingRepository.findAllByProviderIdAndStatus(providerId, status)
    }

    fun findUpcomingBookings(): List<Booking> {
        val now = LocalDateTime.now()
        val endDate = now.plusDays(7)
        return bookingRepository.findUpcomingBookings(now, endDate)
    }

    @Transactional
    fun createBooking(serviceId: Long, userId: Long, startTime: LocalDateTime): Booking {
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { NoSuchElementException("Service not found with id $serviceId") }

        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found with id $userId") }

        // Calculate end time based on service duration
        val endTime = startTime.plusMinutes(service.duration.toLong())

        // Check if the time slot is available
        if (!availabilityService.isTimeSlotAvailable(serviceId, startTime, endTime)) {
            throw BookingException("The requested time slot is not available")
        }

        // Create the booking
        val booking = Booking(
            startTime = startTime,
            endTime = endTime,
            status = BookingStatus.PENDING,
            user = user,
            service = service
        )

        val savedBooking = bookingRepository.save(booking)

        // Create notification for booking confirmation
        val notification = Notification(
            title = "Booking Confirmation",
            content = "Your booking for ${service.name} has been created and is pending confirmation.",
            type = NotificationType.EMAIL,
            booking = savedBooking
        )

        notificationRepository.save(notification)

        return savedBooking
    }

    @Transactional
    fun updateBookingStatus(id: Long, status: BookingStatus): Booking {
        val booking = bookingRepository.findById(id)
            .orElseThrow { NoSuchElementException("Booking not found with id $id") }

        // Validate status transition
        when (status) {
            BookingStatus.CONFIRMED -> {
                if (booking.status != BookingStatus.PENDING) {
                    throw BookingException("Only pending bookings can be confirmed")
                }

                // Send confirmation email
                emailService.sendBookingConfirmationEmail(booking)

                // Create reminder notification (for 24 hours before)
                if (booking.startTime.isAfter(LocalDateTime.now().plusHours(24))) {
                    val reminderNotification = Notification(
                        title = "Booking Reminder",
                        content = "Reminder for your upcoming booking for ${booking.service.name}",
                        type = NotificationType.EMAIL,
                        booking = booking
                    )
                    notificationRepository.save(reminderNotification)
                }
            }

            BookingStatus.CANCELLED -> {
                if (booking.status == BookingStatus.COMPLETED) {
                    throw BookingException("Completed bookings cannot be cancelled")
                }
                // Send cancellation email
                emailService.sendBookingCancellationEmail(booking)
            }

            BookingStatus.COMPLETED -> {
                if (booking.status != BookingStatus.CONFIRMED) {
                    throw BookingException("Only confirmed bookings can be marked as completed")
                }
            }

            else -> {}
        }

        booking.status = status
        return bookingRepository.save(booking)
    }

    @Transactional
    fun updateBookingNotes(id: Long, notes: String): Booking {
        val booking = bookingRepository.findById(id)
            .orElseThrow { NoSuchElementException("Booking not found with id $id") }

        booking.notes = notes
        return bookingRepository.save(booking)
    }

    @Transactional
    fun deleteBooking(id: Long) {
        bookingRepository.deleteById(id)
    }
}