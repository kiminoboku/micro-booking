package online.kimino.micro.booking.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "exception_periods")
data class ExceptionPeriod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var startTime: LocalDateTime,

    @Column(nullable = false)
    var endTime: LocalDateTime,

    @Column(nullable = false)
    var description: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: User,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isValid(): Boolean = endTime.isAfter(startTime)
}