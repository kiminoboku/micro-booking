package online.kimino.micro.booking.config

import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.repository.BookingRepository
import online.kimino.micro.booking.repository.CyclicBookingRepository
import online.kimino.micro.booking.repository.NotificationRepository
import online.kimino.micro.booking.repository.VerificationTokenRepository
import online.kimino.micro.booking.service.CyclicBookingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ScheduledTasks(
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val cyclicBookingRepository: CyclicBookingRepository,
    private val cyclicBookingService: CyclicBookingService
) {

    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    /**
     * Clean up expired verification tokens daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        logger.info("Running scheduled task: cleanupExpiredTokens")

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

    /**
     * Generate new bookings for confirmed cyclic bookings
     * Run once a week on Sunday at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 ? * SUN")
    @Transactional
    fun generateNewCyclicBookings() {
        logger.info("Running scheduled task: generateNewCyclicBookings")

        // Find all confirmed cyclic bookings that are still active (end date is null or in the future)
        val now = LocalDateTime.now()
        val activeCyclicBookings = cyclicBookingRepository.findAll()
            .filter {
                it.status == online.kimino.micro.booking.entity.CyclicBookingStatus.CONFIRMED &&
                        (it.endDate == null || !it.endDate!!.isBefore(now.toLocalDate()))
            }

        if (activeCyclicBookings.isNotEmpty()) {
            logger.info("Generating new bookings for ${activeCyclicBookings.size} active cyclic bookings")

            activeCyclicBookings.forEach { cyclicBooking ->
                try {
                    // Generate new bookings for the next period
                    cyclicBookingService.generateBookings(cyclicBooking)
                    logger.debug("Generated new bookings for cyclic booking ID: ${cyclicBooking.id}")
                } catch (e: Exception) {
                    logger.error("Error generating bookings for cyclic booking ID: ${cyclicBooking.id}", e)
                }
            }
        }
    }
}