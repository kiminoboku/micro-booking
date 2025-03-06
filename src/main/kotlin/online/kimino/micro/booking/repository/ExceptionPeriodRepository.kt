package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.ExceptionPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExceptionPeriodRepository : JpaRepository<ExceptionPeriod, Long> {
    fun findAllByProviderId(providerId: Long): List<ExceptionPeriod>

    @Query("SELECT e FROM ExceptionPeriod e WHERE e.provider.id = :providerId AND ((e.startTime <= :endTime AND e.endTime >= :startTime))")
    fun findOverlappingExceptionPeriods(
        providerId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ExceptionPeriod>
}