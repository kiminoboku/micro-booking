package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.Notification
import online.kimino.micro.booking.entity.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByBookingId(bookingId: Long): List<Notification>

    fun findAllBySentFalseAndType(type: NotificationType): List<Notification>

    @Query("SELECT n FROM Notification n WHERE n.sent = false AND n.booking.startTime <= :minutes")
    fun findPendingNotificationsForUpcomingBookings(minutes: Long): List<Notification>
}