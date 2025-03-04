package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.Notification
import online.kimino.micro.booking.entity.NotificationType
import online.kimino.micro.booking.repository.NotificationRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val emailService: EmailService
) {
    @Transactional
    fun createNotification(notification: Notification): Notification {
        return notificationRepository.save(notification)
    }

    @Transactional
    fun createBookingNotification(
        booking: Booking,
        title: String,
        content: String,
        type: NotificationType
    ): Notification {
        val notification = Notification(
            title = title,
            content = content,
            type = type,
            booking = booking
        )
        return notificationRepository.save(notification)
    }

    @Transactional
    @Scheduled(fixedRate = 60000) // Run every minute
    fun processEmailNotifications() {
        val pendingEmailNotifications = notificationRepository.findAllBySentFalseAndType(NotificationType.EMAIL)

        for (notification in pendingEmailNotifications) {
            val booking = notification.booking

            if (booking != null) {
                when {
                    notification.title.contains("Confirmation") -> {
                        emailService.sendBookingConfirmationEmail(booking)
                    }

                    notification.title.contains("Cancellation") -> {
                        emailService.sendBookingCancellationEmail(booking)
                    }

                    notification.title.contains("Reminder") -> {
                        // Only send reminder if the booking is within 24 hours
                        if (booking.startTime.isBefore(LocalDateTime.now().plusHours(24))) {
                            emailService.sendBookingReminderEmail(booking)
                        } else {
                            // Skip for now, will be processed later
                            continue
                        }
                    }
                }
            }

            // Mark as sent
            notification.sent = true
            notification.sentAt = LocalDateTime.now()
            notificationRepository.save(notification)
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * ?") // Run every day at 8 AM
    fun sendDailyBookingReminders() {
        // Get all bookings that are happening tomorrow
        val tomorrow = LocalDateTime.now().plusDays(1)
        val dayAfterTomorrow = LocalDateTime.now().plusDays(2)

        // Find all pending notifications for bookings in the next 24 hours
        val reminderNotifications = notificationRepository.findPendingNotificationsForUpcomingBookings(24 * 60)

        for (notification in reminderNotifications) {
            val booking = notification.booking

            if (booking != null) {
                emailService.sendBookingReminderEmail(booking)

                // Mark as sent
                notification.sent = true
                notification.sentAt = LocalDateTime.now()
                notificationRepository.save(notification)
            }
        }
    }
}