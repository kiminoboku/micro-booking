package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.Availability
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.DayOfWeek

@Repository
interface AvailabilityRepository : JpaRepository<Availability, Long> {
    fun findAllByServiceId(serviceId: Long): List<Availability>
    fun findAllByServiceIdAndDayOfWeek(serviceId: Long, dayOfWeek: DayOfWeek): List<Availability>

    @Query("SELECT a FROM Availability a WHERE a.service.id = :serviceId AND a.service.active = true")
    fun findAllByActiveServiceId(serviceId: Long): List<Availability>
}