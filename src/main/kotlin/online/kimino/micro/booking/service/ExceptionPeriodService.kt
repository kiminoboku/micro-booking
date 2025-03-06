package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.ExceptionPeriod
import online.kimino.micro.booking.repository.ExceptionPeriodRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class ExceptionPeriodService(
    private val exceptionPeriodRepository: ExceptionPeriodRepository
) {
    fun findById(id: Long): Optional<ExceptionPeriod> {
        return exceptionPeriodRepository.findById(id)
    }

    fun findAllByProviderId(providerId: Long): List<ExceptionPeriod> {
        return exceptionPeriodRepository.findAllByProviderId(providerId)
    }

    fun findOverlappingExceptionPeriods(
        providerId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ExceptionPeriod> {
        return exceptionPeriodRepository.findOverlappingExceptionPeriods(providerId, startTime, endTime)
    }

    @Transactional
    fun createExceptionPeriod(exceptionPeriod: ExceptionPeriod): ExceptionPeriod {
        if (!exceptionPeriod.isValid()) {
            throw IllegalArgumentException("End time must be after start time")
        }
        return exceptionPeriodRepository.save(exceptionPeriod)
    }

    @Transactional
    fun updateExceptionPeriod(exceptionPeriod: ExceptionPeriod): ExceptionPeriod {
        if (!exceptionPeriod.isValid()) {
            throw IllegalArgumentException("End time must be after start time")
        }
        return exceptionPeriodRepository.save(exceptionPeriod)
    }

    @Transactional
    fun deleteExceptionPeriod(id: Long) {
        exceptionPeriodRepository.deleteById(id)
    }

    /**
     * Check if a time slot falls within any exception period for a provider
     */
    fun isWithinExceptionPeriod(providerId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        val overlappingExceptions = findOverlappingExceptionPeriods(providerId, startTime, endTime)
        return overlappingExceptions.isNotEmpty()
    }
}