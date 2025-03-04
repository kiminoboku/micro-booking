package online.kimino.micro.booking.config

import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.repository.BookingRepository
import online.kimino.micro.booking.repository.NotificationRepository
import online.kimino.micro.booking.repository.VerificationTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ScheduledTasks(
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository,
    private val verificationTokenRepository: VerificationTokenRepository
) {

    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    /**
     * Clean up expired verification tokens daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        logger.info("Running scheduled task: cleanupExpiredTokens")

        val now = LocalDateTime.now()
        val expiredTokens = verificationTokenRepository.findAll()
            .filter { it.isExpired() }

        if (expiredTokens.isNotEmpty()) {
            logger.info("Deleting ${expiredTokens.size} expired verification tokens")
            verificationTokenRepository.deleteAll(expiredTokens)
        }
    }

    /**
     * Auto-complete confirmed bookings that are more than 24 hours in the past
     */
    @Scheduled(cron = "0 0 * * * ?") // Run every hour
    @Transactional
    fun autoCompleteOldBookings() {
        logger.info("Running scheduled task: autoCompleteOldBookings")

        val cutoffTime = LocalDateTime.now().minusHours(24)

        val confirmedPastBookings = bookingRepository.findAll()
            .filter { it.status == BookingStatus.CONFIRMED && it.endTime.isBefore(cutoffTime) }

        if (confirmedPastBookings.isNotEmpty()) {
            logger.info("Auto-completing ${confirmedPastBookings.size} past bookings")

            confirmedPastBookings.forEach { booking ->
                booking.status = BookingStatus.COMPLETED
                booking.updatedAt = LocalDateTime.now()
                bookingRepository.save(booking)

                logger.debug("Auto-completed booking ID: ${booking.id}")
            }
        }
    }

    /**
     * Clean up old notifications (keep last 30 days only)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    fun cleanupOldNotifications() {
        logger.info("Running scheduled task: cleanupOldNotifications")

        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)

        val oldNotifications = notificationRepository.findAll()
            .filter { it.createdAt.isBefore(thirtyDaysAgo) }

        if (oldNotifications.isNotEmpty()) {
            logger.info("Deleting ${oldNotifications.size} old notifications")
            notificationRepository.deleteAll(oldNotifications)
        }
    }

    /**
     * Auto-cancel pending bookings older than 24 hours
     */
    @Scheduled(cron = "0 30 * * * ?") // Run every hour at 30 minutes past
    @Transactional
    fun autoCancelOldPendingBookings() {
        logger.info("Running scheduled task: autoCancelOldPendingBookings")

        val cutoffTime = LocalDateTime.now().minusHours(24)

        val oldPendingBookings = bookingRepository.findAll()
            .filter { it.status == BookingStatus.PENDING && it.createdAt.isBefore(cutoffTime) }

        if (oldPendingBookings.isNotEmpty()) {
            logger.info("Auto-cancelling ${oldPendingBookings.size} old pending bookings")

            oldPendingBookings.forEach { booking ->
                booking.status = BookingStatus.CANCELLED
                booking.updatedAt = LocalDateTime.now()
                booking.notes = (booking.notes ?: "") + "\nAutomatically cancelled after 24 hours pending."
                bookingRepository.save(booking)

                logger.debug("Auto-cancelled booking ID: ${booking.id}")
            }
        }
    }
}