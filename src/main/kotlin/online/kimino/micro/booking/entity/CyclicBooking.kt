package online.kimino.micro.booking.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "cyclic_bookings")
data class CyclicBooking(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column
    var endDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var recurrencePattern: RecurrencePattern,

    @Enumerated(EnumType.STRING)
    @Column
    var dayOfWeek: DayOfWeek? = null,  // For weekly recurrence

    @Column
    var dayOfMonth: Int? = null,  // For monthly recurrence

    @Column(nullable = false)
    var startTime: LocalTime,

    @Column(nullable = false)
    var endTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CyclicBookingStatus = CyclicBookingStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    var service: Service,

    @OneToMany(mappedBy = "cyclicBooking", cascade = [CascadeType.ALL], orphanRemoval = true)
    var bookings: MutableList<Booking> = mutableListOf(),

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class RecurrencePattern {
    WEEKLY, MONTHLY
}

enum class CyclicBookingStatus {
    PENDING, CONFIRMED, CANCELLED
}